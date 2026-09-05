package com.example.ml

import com.example.model.ChemicalReactionDetail
import com.example.model.DosageRecommendation
import com.example.model.LandSuitabilityRating
import com.example.model.MLClassificationResult
import com.example.model.Season
import com.example.model.SeasonLandSuitability
import com.example.model.WaterParameters
import com.example.model.WaterType
import kotlin.math.exp
import kotlin.math.roundToInt

object WaterMLEngine {

    // Feature statistics for Standard Scaling
    private const val MEAN_PH = 7.15
    private const val STD_PH = 1.35

    private const val MEAN_TDS = 3200.0
    private const val STD_TDS = 4800.0

    private const val MEAN_EC = 4500.0
    private const val STD_EC = 6400.0

    private const val MEAN_TURB = 5.8
    private const val STD_TURB = 6.9

    private const val MEAN_HARDNESS = 240.0
    private const val STD_HARDNESS = 210.0

    private const val MEAN_TOC = 4.2
    private const val STD_TOC = 4.1

    fun computeZScores(params: WaterParameters): Map<String, Double> {
        return mapOf(
            "pH" to ((params.ph - MEAN_PH) / STD_PH),
            "TDS" to ((params.tds - MEAN_TDS) / STD_TDS),
            "EC" to ((params.ec - MEAN_EC) / STD_EC),
            "Turbidity" to ((params.turbidity - MEAN_TURB) / STD_TURB),
            "Hardness" to ((params.hardness - MEAN_HARDNESS) / STD_HARDNESS),
            "Organic/TOC" to ((params.organicCarbon - MEAN_TOC) / STD_TOC)
        )
    }

    fun classifyAndPredict(params: WaterParameters, rawInflowLiters: Double = 10000.0): MLClassificationResult {
        val z = computeZScores(params)

        // Multi-Class classification scoring (Dual-model pipeline / Ensemble Tree Matrix)
        var scoreA = 0.0 // Freshwater
        var scoreB = 0.0 // Brackish
        var scoreC = 0.0 // Agri Runoff
        var scoreD = 0.0 // Acidic Mine
        var scoreE = 0.0 // Highly Saline

        // 1. pH evaluation
        if (params.ph < 6.0) {
            scoreD += (6.0 - params.ph) * 4.5 + 3.0
        } else if (params.ph in 6.5..8.5) {
            scoreA += 2.8
            scoreB += 1.5
            scoreC += 1.2
        } else {
            scoreC += 1.8
        }

        // 2. TDS evaluation
        if (params.tds > 10000.0) {
            scoreE += (params.tds / 4000.0) + 4.0
        } else if (params.tds in 1000.0..5000.0) {
            scoreB += 4.5
        } else if (params.tds < 500.0) {
            scoreA += 4.8
        } else if (params.tds in 500.0..1000.0) {
            scoreA += 1.5
            scoreB += 2.0
            scoreC += 1.8
        }

        // 3. EC evaluation
        if (params.ec > 15000.0) {
            scoreE += 3.8
        } else if (params.ec in 1600.0..8000.0) {
            scoreB += 2.2
        } else if (params.ec < 800.0) {
            scoreA += 2.0
        }

        // 4. Turbidity evaluation
        if (params.turbidity > 5.0) {
            scoreC += (params.turbidity * 0.8) + 3.5
        } else if (params.turbidity < 2.0) {
            scoreA += 1.5
            scoreE += 1.0
        }

        // 5. Hardness evaluation
        if (params.hardness > 250.0) {
            scoreB += 2.5
            scoreD += 1.2
        } else if (params.hardness < 120.0) {
            scoreA += 1.2
        }

        // 6. Organic Carbon / Chloramines
        if (params.organicCarbon > 4.5) {
            scoreC += 3.0
        }

        // Apply softmax normalization
        val maxScore = maxOf(scoreA, scoreB, scoreC, scoreD, scoreE)
        val expA = exp(scoreA - maxScore)
        val expB = exp(scoreB - maxScore)
        val expC = exp(scoreC - maxScore)
        val expD = exp(scoreD - maxScore)
        val expE = exp(scoreE - maxScore)
        val sumExp = expA + expB + expC + expD + expE

        val probMap = mapOf(
            WaterType.TYPE_A to (expA / sumExp).toFloat(),
            WaterType.TYPE_B to (expB / sumExp).toFloat(),
            WaterType.TYPE_C to (expC / sumExp).toFloat(),
            WaterType.TYPE_D to (expD / sumExp).toFloat(),
            WaterType.TYPE_E to (expE / sumExp).toFloat()
        )

        val predictedType = probMap.maxByOrNull { it.value }?.key ?: WaterType.TYPE_A
        val confidence = probMap[predictedType] ?: 0.95f

        // Regression Pipeline: Yield & Volume Output
        val predictedYieldPercent = when (predictedType) {
            WaterType.TYPE_A -> {
                // 95% - 98%
                val penalty = (params.turbidity * 0.4).coerceAtMost(3.0)
                (98.0 - penalty).coerceIn(94.0, 98.5)
            }
            WaterType.TYPE_B -> {
                // 70% - 80%
                val salinityPenalty = ((params.tds - 1000.0) / 400.0).coerceIn(0.0, 10.0)
                (80.0 - salinityPenalty).coerceIn(70.0, 81.0)
            }
            WaterType.TYPE_C -> {
                // 85% - 90%
                val turbPenalty = ((params.turbidity - 5.0) * 0.5).coerceIn(0.0, 5.0)
                (90.0 - turbPenalty).coerceIn(84.0, 91.0)
            }
            WaterType.TYPE_D -> {
                // 90%
                90.0
            }
            WaterType.TYPE_E -> {
                // 35% - 45%
                val osmoticDeduction = ((params.tds - 10000.0) / 2500.0).coerceIn(0.0, 10.0)
                (45.0 - osmoticDeduction).coerceIn(34.0, 46.0)
            }
        }

        val treatableVolume = (rawInflowLiters * (predictedYieldPercent / 100.0))
        val convertibilityScore = when (predictedType) {
            WaterType.TYPE_A -> (94 + (confidence * 5)).roundToInt().coerceIn(90, 99)
            WaterType.TYPE_B -> (76 + (confidence * 6)).roundToInt().coerceIn(70, 84)
            WaterType.TYPE_C -> (84 + (confidence * 7)).roundToInt().coerceIn(80, 92)
            WaterType.TYPE_D -> (78 + (confidence * 10)).roundToInt().coerceIn(72, 90)
            WaterType.TYPE_E -> (42 + (confidence * 8)).roundToInt().coerceIn(35, 52)
        }

        // Energy requirement regression (kWh per m³)
        val energyKWh = when (predictedType) {
            WaterType.TYPE_A -> 0.35 + (params.turbidity * 0.04)
            WaterType.TYPE_B -> 1.40 + (params.tds / 3500.0)
            WaterType.TYPE_C -> 0.95 + (params.turbidity * 0.06)
            WaterType.TYPE_D -> 1.20 + (7.0 - params.ph).coerceAtLeast(0.0) * 0.25
            WaterType.TYPE_E -> 3.20 + (params.tds / 8000.0)
        }

        // Chemical & Treatment rules
        val reactions = getChemicalReactionsForType(predictedType, params)
        val dosages = getDosageRecommendations(predictedType, params)
        val remediationSteps = getRemediationSteps(predictedType)
        val seasonalSuitabilities = computeSeasonalLandSuitability(predictedType, params)

        return MLClassificationResult(
            waterType = predictedType,
            confidence = confidence,
            convertibilityScore = convertibilityScore,
            predictedYieldPercent = predictedYieldPercent,
            treatableVolumeLiters = treatableVolume,
            energyRequirementKWh = energyKWh,
            zScores = z,
            probabilities = probMap,
            chemicalReactions = reactions,
            dosages = dosages,
            remediationSteps = remediationSteps,
            seasonalSuitabilities = seasonalSuitabilities
        )
    }

    private fun getChemicalReactionsForType(type: WaterType, params: WaterParameters): List<ChemicalReactionDetail> {
        val list = mutableListOf<ChemicalReactionDetail>()

        when (type) {
            WaterType.TYPE_D -> {
                list.add(
                    ChemicalReactionDetail(
                        title = "Hydrated Lime Neutralization",
                        equation = "Ca(OH)₂ + H₂SO₄ → CaSO₄↓ + 2H₂O",
                        reactants = "Hydrated Lime slurry Ca(OH)₂ + Sulfuric acid groundwater H₂SO₄",
                        products = "Insoluble Calcium Sulfate dihydrate (Gypsum) CaSO₄·2H₂O + Neutralized Water",
                        mechanism = "Basic hydroxide ions (OH⁻) neutralize hydronium (H₃O⁺) while calcium precipitates toxic sulfates as insoluble gypsum flocs.",
                        residueHandling = "Lamella clarifier underflow discharged to mechanical belt filter press; produces dry gypsum cake (reusable in cement/agriculture)."
                    )
                )
                list.add(
                    ChemicalReactionDetail(
                        title = "Heavy Metal Precipitation",
                        equation = "Fe²⁺ + 2OH⁻ + ½O₂ + H₂O → 2Fe(OH)₃↓",
                        reactants = "Dissolved Ferrous/Heavy metal ions + Dissolved atmospheric Oxygen + Lime alkalinity",
                        products = "Ferric hydroxide precipitate (red-brown floc)",
                        mechanism = "Oxidation followed by alkaline precipitation shifts metal solubility product to near-zero at pH 8.2.",
                        residueHandling = "Flocculation chamber polymer addition facilitates rapid settling in settling ponds."
                    )
                )
            }
            WaterType.TYPE_C -> {
                list.add(
                    ChemicalReactionDetail(
                        title = "Alum Coagulation & Floc Formation",
                        equation = "Al₂(SO₄)₃·14H₂O + 3Ca(HCO₃)₂ → 2Al(OH)₃↓ + 3CaSO₄ + 14H₂O + 6CO₂",
                        reactants = "Commercial Aluminum Sulfate (Alum) + Bicarbonate alkalinity",
                        products = "Gelatinous Aluminum Hydroxide flocs Al(OH)₃ + Dissolved Calcium Sulfate + Carbon dioxide",
                        mechanism = "Trivalent aluminum ions (Al³⁺) compress the electrical double layer of negatively charged colloidal clay/algae, sweeping suspended particles into settleable flocs.",
                        residueHandling = "Sludge pumped to aerobic digestion drying beds; filtered effluent proceeds to carbon adsorption."
                    )
                )
                list.add(
                    ChemicalReactionDetail(
                        title = "Granular Activated Carbon (GAC) Adsorption",
                        equation = "C* + NH₂Cl + H₂O → C*O + NH₄⁺ + Cl⁻",
                        reactants = "Porous Activated Carbon surface sites C* + Chloramines / Organic Carbon Pesticides",
                        products = "Oxidized Carbon Surface C*O + Ammonium NH₄⁺ + Harmless Chloride Cl⁻",
                        mechanism = "High internal surface area (>1,000 m²/g) provides microporous chemisorption of organophosphates and chloramines.",
                        residueHandling = "Periodic thermal reactivation at 850°C to restore adsorption capacity."
                    )
                )
            }
            WaterType.TYPE_B -> {
                list.add(
                    ChemicalReactionDetail(
                        title = "Ion-Exchange Resin Softening",
                        equation = "2(R-Na) + Ca²⁺(aq) → R₂-Ca + 2Na⁺(aq)",
                        reactants = "Sodium-form strong acid cation resin (R-Na) + Hardness ions (Ca²⁺, Mg²⁺)",
                        products = "Exhausted calcium-bound resin bed + Softened water containing sodium ions",
                        mechanism = "Polymeric polystyrene matrix selectively binds divalent alkaline earth cations, preventing scaling on downstream reverse osmosis membranes.",
                        residueHandling = "Counter-current regeneration with saturated brine (10% NaCl); rinse water safely reclaimed."
                    )
                )
                list.add(
                    ChemicalReactionDetail(
                        title = "Reverse Osmosis Selective Permeation",
                        equation = "J_w = A (ΔP - ΔΠ)",
                        reactants = "Pressurized Brackish Feed Water (12 - 18 bar)",
                        products = "High-purity Potable Permeate (TDS < 150 ppm) + Concentrated Reject Brine",
                        mechanism = "Polyamide thin-film composite membrane exerts solution-diffusion transport; salt ions rejected by Donnan exclusion and steric hindrance.",
                        residueHandling = "Brine stream utilized for salt-tolerant halophyte crop irrigation or solar evaporation pans."
                    )
                )
            }
            WaterType.TYPE_E -> {
                list.add(
                    ChemicalReactionDetail(
                        title = "High-Pressure Seawater Desalination",
                        equation = "ΔP_applied > 65 bar (Π_osmotic ≈ 28 bar)",
                        reactants = "Hyper-saline seawater (TDS > 35,000 ppm) + Phosphonate Antiscalant",
                        products = "Pure Potable Desalinated Water + High-Density Reject Concentrate",
                        mechanism = "Exceeds natural osmotic barrier by applying hydraulic pressures up to 70 bar, forcing water molecules across semipermeable spirally wound membranes.",
                        residueHandling = "Isobaric energy recovery devices (ERD) recover 95% of hydraulic energy from brine; brine diffused safely via multi-port ocean outfall."
                    )
                )
                list.add(
                    ChemicalReactionDetail(
                        title = "Antiscalant Complexation Reaction",
                        equation = "Ca²⁺ + SO₄²⁻ + [P-Ligand] → [Ca-Ligand] (Soluble Non-Crystalline)",
                        reactants = "Supersaturated calcium/sulfate/carbonate species + Organophosphonate antiscalant",
                        products = "Sub-stoichiometric chelate complex preventing crystal lattice nucleation",
                        mechanism = "Threshold inhibition deforms scale crystallites, delaying precipitation kinetics beyond membrane residence time.",
                        residueHandling = "Continuously flushed with concentrate reject stream."
                    )
                )
            }
            WaterType.TYPE_A -> {
                list.add(
                    ChemicalReactionDetail(
                        title = "UV-C Photolytic DNA/RNA Inactivation",
                        equation = "Thymine + Thymine + hν (254 nm) → Thymine-Thymine Cyclobutane Dimer",
                        reactants = "Active pathogenic bacteria/viruses + Germicidal UV-C Photons (254 nm)",
                        products = "Inactivated non-replicating microbial genomes",
                        mechanism = "Photochemical absorption creates covalent intrastrand cyclobutane pyrimidine dimers, blocking polymerase transcription.",
                        residueHandling = "Zero chemical residue; zero disinfection by-products (DBPs like trihalomethanes)."
                    )
                )
                list.add(
                    ChemicalReactionDetail(
                        title = "Depth Sediment Micro-Filtration",
                        equation = "Suspended Solids (> 5 μm) + Polypropylene Matrix → Trapped Cake",
                        reactants = "Raw freshwater feed + Spun polypropylene melt-blown cartridge",
                        products = "Crystal-clear optical effluent (Turbidity < 0.2 NTU)",
                        mechanism = "Tortuous path mechanical sieving, inertial impaction, and surface adsorption.",
                        residueHandling = "Automated Differential Pressure (ΔP) backwash flush every 48 hours."
                    )
                )
            }
        }
        return list
    }

    private fun getDosageRecommendations(type: WaterType, params: WaterParameters): List<DosageRecommendation> {
        val dosages = mutableListOf<DosageRecommendation>()
        when (type) {
            WaterType.TYPE_D -> {
                val limeDoseGramsM3 = ((7.2 - params.ph) * 140.0).coerceAtLeast(35.0)
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Hydrated Lime (Ca(OH)₂ 92% purity)",
                        purpose = "pH neutralization and heavy metal precipitation",
                        dosageRate = "${limeDoseGramsM3.roundToInt()}",
                        unit = "g/m³ feed",
                        storageSpec = "Dry silo with fluidizing air pads and slurry mixer"
                    )
                )
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Anionic Polyacrylamide Flocculant",
                        purpose = "Agglomeration of gypsum and iron flocs",
                        dosageRate = "0.8 - 1.5",
                        unit = "mg/L (ppm)",
                        storageSpec = "Liquid polymer prep skid, 0.1% aging solution"
                    )
                )
            }
            WaterType.TYPE_C -> {
                val alumDoseMgL = (14.0 + (params.turbidity * 2.5)).coerceAtMost(65.0)
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Aluminum Sulfate Al₂(SO₄)₃·14H₂O",
                        purpose = "Turbidity destabilization & coagulant charge neutralization",
                        dosageRate = "${alumDoseMgL.roundToInt()}",
                        unit = "mg/L",
                        storageSpec = "Rubber-lined tanks, diaphragm metering pumps"
                    )
                )
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Granular Activated Carbon Replacement",
                        purpose = "Removal of organochlorines, nitrates, and pesticide residues",
                        dosageRate = "0.08",
                        unit = "kg/m³ treated",
                        storageSpec = "Epoxy-lined pressure vessels, backwash expansion 40%"
                    )
                )
            }
            WaterType.TYPE_B -> {
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "RO Membrane Antiscalant (Phosphonate blend)",
                        purpose = "Inhibit calcium carbonate and silica precipitation on membranes",
                        dosageRate = "3.2",
                        unit = "ppm continuous",
                        storageSpec = "HDPE storage drum, digital dosing pump"
                    )
                )
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Sodium Chloride (NaCl 99.5% pure)",
                        purpose = "Cation exchange water softener bed regeneration",
                        dosageRate = "120.0",
                        unit = "g per liter resin",
                        storageSpec = "Brine saturator tank with level sensors"
                    )
                )
            }
            WaterType.TYPE_E -> {
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "High-Performance Polyamide Antiscalant",
                        purpose = "Prevent barite and fluorite crystallization at 70 bar",
                        dosageRate = "4.5",
                        unit = "ppm",
                        storageSpec = "Direct dosing with dual redundant pump train"
                    )
                )
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Sodium Metabisulfite (SMBS)",
                        purpose = "Dechlorination to protect delicate polyamide membranes",
                        dosageRate = "2.0",
                        unit = "ppm per ppm free chlorine",
                        storageSpec = "Nitrogen-blanketed feed tank"
                    )
                )
            }
            WaterType.TYPE_A -> {
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "UV Germicidal Irradiation Dose",
                        purpose = "Pathogen DNA inactivation (Bacteria, Cryptosporidium, Giardia)",
                        dosageRate = "40.0",
                        unit = "mJ/cm² fluence",
                        storageSpec = "Medium-pressure quartz sleeve UV reactor"
                    )
                )
                dosages.add(
                    DosageRecommendation(
                        chemicalName = "Sodium Hypochlorite (Residual)",
                        purpose = "Secondary distribution pipeline residual disinfection",
                        dosageRate = "0.5 - 1.0",
                        unit = "ppm as free Cl₂",
                        storageSpec = "Cool ventilated chemical storage room"
                    )
                )
            }
        }
        return dosages
    }

    private fun getRemediationSteps(type: WaterType): List<String> {
        return when (type) {
            WaterType.TYPE_A -> listOf(
                "Stage 1: Pre-filtration through 20-micron sediment cartridge to remove coarse particulates.",
                "Stage 2: 5-micron spun polypropylene polishing stage for sub-micron turbidity reduction.",
                "Stage 3: High-intensity UV-C disinfection reactor chamber (254 nm waveband) delivering >40 mJ/cm² fluence.",
                "Stage 4: Residual chlorination at 0.5 ppm for distribution hygiene assurance."
            )
            WaterType.TYPE_B -> listOf(
                "Stage 1: Multi-media bed filtration (anthracite, sand, garnet) to achieve SDI < 3.0.",
                "Stage 2: Continuous antiscalant infusion prior to high-pressure pump.",
                "Stage 3: 5-micron safety security filter protecting the RO pressure vessel train.",
                "Stage 4: Two-stage Reverse Osmosis pass with 75% system recovery and concentrate energy recovery.",
                "Stage 5: Post-treatment remineralization and pH stabilization for irrigation or drinking."
            )
            WaterType.TYPE_C -> listOf(
                "Stage 1: Rapid mixing coagulation tank with automated Alum dosing based on turbidity sensors.",
                "Stage 2: Slow-stirring flocculation basin with polymer aid promoting dense floc growth.",
                "Stage 3: High-rate lamella plate settler for gravity sludge separation.",
                "Stage 4: Dual-media sand and Granular Activated Carbon (GAC) filtration to strip pesticides & TOC.",
                "Stage 5: Nitrate bio-filtration and UV disinfection for safe agricultural reuse."
            )
            WaterType.TYPE_D -> listOf(
                "Stage 1: Cascading aeration weir to oxidize ferrous iron (Fe²⁺) to ferric (Fe³⁺).",
                "Stage 2: Lime slurry dosing tank regulated by real-time closed-loop pH controllers targeting 8.2.",
                "Stage 3: Heavy metal and gypsum precipitation in high-density sludge (HDS) reactor.",
                "Stage 4: Clarification clarifier separating clear industrial supernatant from slurry underflow.",
                "Stage 5: Belt press sludge dewatering producing dry cake for industrial recycling."
            )
            WaterType.TYPE_E -> listOf(
                "Stage 1: Dual-stage deep-bed seawater filtration with coagulation aid.",
                "Stage 2: Ultrafiltration (UF) membrane pre-treatment ensuring Silt Density Index (SDI) < 2.5.",
                "Stage 3: High-pressure booster pump producing 65 - 75 bar hydraulic pressure.",
                "Stage 4: SWRO membrane separation with isobaric pressure exchanger recovering 96% waste energy.",
                "Stage 5: Calcite contactor post-treatment for alkalinity and calcium hardness re-balancing."
            )
        }
    }

    fun computeSeasonalLandSuitability(type: WaterType, params: WaterParameters): List<SeasonLandSuitability> {
        return when (type) {
            WaterType.TYPE_A -> listOf(
                SeasonLandSuitability(
                    season = Season.SUMMER,
                    rating = LandSuitabilityRating.EXCELLENT,
                    headline = "Excellent for All Soils & Crops in Dry Heat",
                    soilImpact = "Zero salt buildup. Soil maintains high water infiltration and aeration under summer sun.",
                    irrigationGuide = "Water during early morning or evening hours. Surface mulching preserves root moisture effectively.",
                    recommendedCrops = listOf("Strawberries", "Tomatoes", "Avocados", "Melons", "Almonds"),
                    evaporationRisk = "Moderate",
                    extraWateringNeededPercent = 10
                ),
                SeasonLandSuitability(
                    season = Season.MONSOON,
                    rating = LandSuitabilityRating.EXCELLENT,
                    headline = "Safe for Replenishing Farm Soil & Water Tables",
                    soilImpact = "Compatible with natural rainwater. Zero sodium or chemical risk when blending with saturated soils.",
                    irrigationGuide = "Ensure field drainage furrows are clear to prevent waterlogging during continuous heavy downpours.",
                    recommendedCrops = listOf("Paddy Rice", "Corn / Maize", "Soybeans", "Leafy Greens"),
                    evaporationRisk = "Very Low",
                    extraWateringNeededPercent = 0
                ),
                SeasonLandSuitability(
                    season = Season.AUTUMN,
                    rating = LandSuitabilityRating.EXCELLENT,
                    headline = "Optimal Post-Monsoon Soil Conditioning",
                    soilImpact = "Balances soil moisture, maintains micro-nutrients, and stimulates active nitrogen-fixing microbes.",
                    irrigationGuide = "Regular scheduled drip or furrow irrigation without risk of mineral crusting.",
                    recommendedCrops = listOf("Wheat", "Autumn Legumes", "Root Vegetables", "Fruit Orchards"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 5
                ),
                SeasonLandSuitability(
                    season = Season.WINTER,
                    rating = LandSuitabilityRating.EXCELLENT,
                    headline = "Safe for Cold-Weather Growth & Dormancy",
                    soilImpact = "Cold soil retains low-salinity moisture cleanly without white salt rings or root chill shock.",
                    irrigationGuide = "Reduce irrigation volume by 35-40% to match slower plant evapotranspiration in winter.",
                    recommendedCrops = listOf("Winter Wheat", "Barley", "Mustard", "Spinach", "Carrots"),
                    evaporationRisk = "Minimal",
                    extraWateringNeededPercent = 0
                )
            )

            WaterType.TYPE_B -> listOf(
                SeasonLandSuitability(
                    season = Season.SUMMER,
                    rating = LandSuitabilityRating.CAUTION,
                    headline = "High Heat Evaporation Concentrates Salts on Topsoil",
                    soilImpact = "Intense sun causes capillary rise of salts, forming a white crust. Dense clay soils risk sodium compaction.",
                    irrigationGuide = "Apply 25-30% extra watering (leaching fraction) to push salts below root depth. Avoid spraying leaves in midday.",
                    recommendedCrops = listOf("Barley", "Cotton", "Date Palms", "Sugar Beet", "Sorghum"),
                    evaporationRisk = "Severe",
                    extraWateringNeededPercent = 30
                ),
                SeasonLandSuitability(
                    season = Season.MONSOON,
                    rating = LandSuitabilityRating.GOOD,
                    headline = "Best Season to Irrigate Land with Brackish Water",
                    soilImpact = "Heavy rainfall naturally dilutes TDS and leaches accumulated root salts deep into subsoil layers.",
                    irrigationGuide = "Irrigate between rain showers; rainwater provides natural dilution and prevents soil sodicity.",
                    recommendedCrops = listOf("Cotton", "Sunflower", "Millets", "Tolerant Rice"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 10
                ),
                SeasonLandSuitability(
                    season = Season.AUTUMN,
                    rating = LandSuitabilityRating.CAUTION,
                    headline = "Drying Autumn Winds Require Soil Salinity Monitoring",
                    soilImpact = "As monsoon rains recede, evaporation resumes and remaining salts can linger in seedbeds.",
                    irrigationGuide = "Test soil salinity before sowing winter crops; blend with rainwater or apply gypsum if soil feels crusted.",
                    recommendedCrops = listOf("Olive", "Sugar Beet", "Soybeans", "Barley"),
                    evaporationRisk = "Moderate",
                    extraWateringNeededPercent = 20
                ),
                SeasonLandSuitability(
                    season = Season.WINTER,
                    rating = LandSuitabilityRating.GOOD,
                    headline = "Cold Soil Slows Surface Salt Crystallization",
                    soilImpact = "Cool temperatures keep minerals dissolved longer in soil solution without intense upward wicking.",
                    irrigationGuide = "Irrigate deeply with wider intervals so cold soil has adequate oxygen between watering cycles.",
                    recommendedCrops = listOf("Winter Barley", "Wheat (tolerant strains)", "Mustard", "Rape Seed"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 15
                )
            )

            WaterType.TYPE_C -> listOf(
                SeasonLandSuitability(
                    season = Season.SUMMER,
                    rating = LandSuitabilityRating.CAUTION,
                    headline = "Warm Farm Water Triggers Organic Algae Soil Films",
                    soilImpact = "Elevated organic carbon (TOC) and nitrates under summer heat promote algae growth that can seal soil pores.",
                    irrigationGuide = "Filter coarse organics before use. Apply under mulch rather than flooding open furrows to prevent algae blankets.",
                    recommendedCrops = listOf("Corn / Maize", "Fodder Grass", "Sunflower", "Sorghum"),
                    evaporationRisk = "High",
                    extraWateringNeededPercent = 15
                ),
                SeasonLandSuitability(
                    season = Season.MONSOON,
                    rating = LandSuitabilityRating.CAUTION,
                    headline = "Muddy Silt Runoff Can Clog Heavy Clay Soils",
                    soilImpact = "Extreme monsoon turbidity (>20 NTU) deposits dense mud layers that impede soil respiration and drainage.",
                    irrigationGuide = "Allow turbid runoff to settle in a farm silt pond for 24 hours before directing onto garden beds.",
                    recommendedCrops = listOf("Paddy", "Sesbania cover crops", "Vetiver grass", "Bamboos"),
                    evaporationRisk = "Very Low",
                    extraWateringNeededPercent = 5
                ),
                SeasonLandSuitability(
                    season = Season.AUTUMN,
                    rating = LandSuitabilityRating.GOOD,
                    headline = "Natural Runoff Nutrients Naturally Enrich Autumn Land",
                    soilImpact = "Mild autumn temperatures let soil microorganisms decompose organic matter into rich humic conditioner.",
                    irrigationGuide = "Excellent for irrigating post-monsoon cover crops and replenishing nutrient-depleted soil.",
                    recommendedCrops = listOf("Wheat", "Mustard", "Silage Sorghum", "Pasture Grass"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 10
                ),
                SeasonLandSuitability(
                    season = Season.WINTER,
                    rating = LandSuitabilityRating.GOOD,
                    headline = "Safe for Winter Soil Conditioning & Grain Fields",
                    soilImpact = "Cool weather prevents fermentation; nitrogen and potassium in runoff stay stable for slow root uptake.",
                    irrigationGuide = "Apply during midday sunshine; periodically flush drip lines to prevent organic sediment clogs.",
                    recommendedCrops = listOf("Winter Cereals", "Legumes", "Root Vegetables", "Alfalfa"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 8
                )
            )

            WaterType.TYPE_D -> listOf(
                SeasonLandSuitability(
                    season = Season.SUMMER,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Dangerous: Hot Acid Water Scorches Roots & Soil",
                    soilImpact = "Acidity (pH < 6.0) attacks soil earthworms, dissolves toxic aluminum, and burns tender plant root hairs.",
                    irrigationGuide = "NEVER apply directly to land! Must neutralize with agricultural lime (Ca(OH)₂) to pH 7.0 before release.",
                    recommendedCrops = listOf("None without lime neutralization (pH < 6.0 locks up all nutrients)"),
                    evaporationRisk = "Severe",
                    extraWateringNeededPercent = 25
                ),
                SeasonLandSuitability(
                    season = Season.MONSOON,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Acid Runoff Hazard to Ground & Downstream Farmland",
                    soilImpact = "Monsoon runoff leaches essential calcium and magnesium from land, leaving soil sour and infertile.",
                    irrigationGuide = "Divert acid streams through crushed limestone filter beds before permitting any soil contact.",
                    recommendedCrops = listOf("Not suitable without lime neutralization"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 10
                ),
                SeasonLandSuitability(
                    season = Season.AUTUMN,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Blocks Soil Nutrient Uptake for Autumn Sowing",
                    soilImpact = "Soil acidity binds phosphates into insoluble complexes, causing newly emerged seedlings to starve.",
                    irrigationGuide = "Treat water in neutralization tank and incorporate agricultural lime into soil before autumn planting.",
                    recommendedCrops = listOf("Only crops on heavily lime-amended soils"),
                    evaporationRisk = "Moderate",
                    extraWateringNeededPercent = 15
                ),
                SeasonLandSuitability(
                    season = Season.WINTER,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Acid Residues Linger in Cold Dormant Soil",
                    soilImpact = "Sluggish cold-weather soil reactions allow heavy metal ions to remain active and toxic around roots all winter.",
                    irrigationGuide = "Full neutralization and aeration settling required prior to winter crop watering.",
                    recommendedCrops = listOf("Neutralized water only"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 12
                )
            )

            WaterType.TYPE_E -> listOf(
                SeasonLandSuitability(
                    season = Season.SUMMER,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Severe Hazard: Turns Fertile Soil into Barren Salt Bed",
                    soilImpact = "Extreme osmotic burn: high salinity pulls water OUT of plants, scorching foliage and destroying soil tilth.",
                    irrigationGuide = "STRICTLY PROHIBITED for normal land irrigation. Must undergo Reverse Osmosis (RO) desalination first.",
                    recommendedCrops = listOf("Tidal Mangroves only (coastal marine wetlands)"),
                    evaporationRisk = "Extreme",
                    extraWateringNeededPercent = 45
                ),
                SeasonLandSuitability(
                    season = Season.MONSOON,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Monsoon Rain Cannot Sufficiently Dilute Seawater",
                    soilImpact = "Excessive sodium ions disperse soil clay particles, creating an airless, water-impermeable hardpan.",
                    irrigationGuide = "Do not discharge onto farmland. Retain within coastal channels or pass through RO membrane systems.",
                    recommendedCrops = listOf("Salicornia (glasswort)", "Salt-marsh halophytes"),
                    evaporationRisk = "Moderate",
                    extraWateringNeededPercent = 35
                ),
                SeasonLandSuitability(
                    season = Season.AUTUMN,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Risk of Permanent Soil Sterilization",
                    soilImpact = "High chloride toxicity permanently destroys earthworms and nitrogen-fixing micro-flora.",
                    irrigationGuide = "Only pure desalinated permeate water can be applied to land.",
                    recommendedCrops = listOf("Desalinated water only"),
                    evaporationRisk = "High",
                    extraWateringNeededPercent = 40
                ),
                SeasonLandSuitability(
                    season = Season.WINTER,
                    rating = LandSuitabilityRating.RESTRICTED,
                    headline = "Salts Trap Moisture as Toxic Brine in Cold Soil",
                    soilImpact = "Prevents seeds from imbibing water; dormant buds fail to sprout in spring due to toxic osmotic tension.",
                    irrigationGuide = "Desalinate with RO membrane filtration before farm or garden use.",
                    recommendedCrops = listOf("Desalinated water only"),
                    evaporationRisk = "Low",
                    extraWateringNeededPercent = 30
                )
            )
        }
    }
}
