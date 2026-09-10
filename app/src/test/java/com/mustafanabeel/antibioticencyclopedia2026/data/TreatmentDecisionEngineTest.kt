package com.mustafanabeel.antibioticencyclopedia2026.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatmentDecisionEngineTest {
    private val syndrome = ReferenceEntry(
        id = "infection-cap",
        kind = "infection",
        title = "Severe CAP",
        fields = listOf(
            ReferenceField("Likely pathogens", "Pneumococcus, MRSA when risk is present"),
            ReferenceField("Empiric regimen", "Ceftriaxone plus azithromycin; add vancomycin for MRSA risk."),
            ReferenceField("Key modifier / action", "Obtain respiratory and blood cultures."),
        ),
        sourceId = "empiric_quick",
        sourceTitle = "Test empiric guide",
        page = 8,
    )
    private val organism = ReferenceEntry(
        id = "organism-mrsa",
        kind = "bacteria",
        title = "Staphylococcus aureus - MRSA",
        fields = listOf(
            ReferenceField("Typical syndromes", "Pneumonia, bacteremia, abscess"),
            ReferenceField("High-yield interpretation", "Standard beta-lactams are inactive."),
        ),
    )
    private val vancomycinSpectrum = ReferenceEntry(
        id = "spectrum-vanco",
        kind = "antibiotic",
        title = "Vancomycin",
        fields = listOf(
            ReferenceField("Gram-positive", "MRSA, CoNS, streptococci"),
            ReferenceField("Major gaps", "All Gram-negatives and VRE"),
        ),
    )
    private val betaLactamSpectrum = ReferenceEntry(
        id = "spectrum-nafcillin",
        kind = "antibiotic",
        title = "Anti-staphylococcal penicillins",
        fields = listOf(
            ReferenceField("Gram-positive", "MSSA and streptococci"),
            ReferenceField("Major gaps", "MRSA and Enterococcus"),
        ),
    )
    private val vancomycinPenetration = ReferenceEntry(
        id = "penetration-vanco",
        kind = "distribution",
        title = "Vancomycin",
        fields = listOf(ReferenceField("Lung", "+/++")),
    )
    private val lungSummary = ReferenceEntry(
        id = "site-lung",
        kind = "infection",
        title = "Lung alveoli",
        text = "Use agents with adequate epithelial lining fluid exposure.",
    )
    private val deepDive = ReferenceEntry(
        id = "deep-mrsa",
        kind = "reference",
        title = "Staphylococcus aureus - MRSA",
        text = "Therapeutic ladder for invasive MRSA infection.",
    )
    private val dataset = EncyclopediaDataset(
        drugs = listOf(
            DrugRecord(id = "ceftriaxone", name = "Ceftriaxone", renalRrt = "Review CrCl."),
            DrugRecord(id = "azithromycin", name = "Azithromycin", renalRrt = "Usually no adjustment."),
            DrugRecord(id = "vancomycin", name = "Vancomycin IV", renalRrt = "AUC-guided dosing."),
        ),
        entries = listOf(
            syndrome,
            organism,
            vancomycinSpectrum,
            betaLactamSpectrum,
            vancomycinPenetration,
            lungSummary,
            deepDive,
        ),
    )
    private val taxonomy = ClinicalTaxonomy(
        infectionGroups = listOf(
            InfectionGroupDefinition(
                id = "respiratory",
                titleAr = "التنفسي",
                titleEn = "Respiratory",
                entryIds = listOf(syndrome.id),
            )
        ),
        organismFamilies = listOf(
            OrganismFamilyDefinition(
                id = "gram_positive_cocci",
                titleAr = "المكورات موجبة الغرام",
                titleEn = "Gram-positive cocci",
                organismEntryIds = listOf(organism.id),
                coverageField = "Gram-positive",
                deepDiveEntryIds = listOf(deepDive.id),
            )
        ),
        tissueSites = listOf(
            TissueSiteDefinition(
                id = "lung",
                titleAr = "الرئة",
                titleEn = "Lung",
                matrixField = "Lung",
                summaryEntryIds = listOf(lungSummary.id),
            )
        ),
        spectrumEntryIds = listOf(vancomycinSpectrum.id, betaLactamSpectrum.id),
        penetrationEntryIds = listOf(vancomycinPenetration.id),
    )
    private val engine = TreatmentDecisionEngine(dataset, taxonomy)

    @Test
    fun exactSyndromeReturnsOnlyTheStoredRegimenAndDrugLinks() {
        val result = engine.evaluate(TreatmentSelection(syndromeEntryId = syndrome.id))

        assertEquals(syndrome.field("Empiric regimen"), result.empiricRegimen)
        assertEquals(setOf("Ceftriaxone", "Azithromycin", "Vancomycin IV"), result.linkedDrugs.map { it.name }.toSet())
        assertTrue(result.relatedSyndromes.isEmpty())
    }

    @Test
    fun siteOnlySuggestsSyndromeInsteadOfInventingRegimen() {
        val result = engine.evaluate(TreatmentSelection(tissueSiteId = "lung"))

        assertTrue(result.empiricRegimen.isBlank())
        assertEquals(listOf(syndrome.id), result.relatedSyndromes.map { it.id })
        assertTrue(result.notes.any { it.titleEn.contains("does not define one regimen") })
    }

    @Test
    fun organismAndSiteKeepMrsaCoverageAndExcludeExplicitMrsaGap() {
        val result = engine.evaluate(
            TreatmentSelection(organismEntryId = organism.id, tissueSiteId = "lung")
        )

        assertTrue(result.classFits.any { it.spectrumEntry.id == vancomycinSpectrum.id })
        assertFalse(result.classFits.any { it.spectrumEntry.id == betaLactamSpectrum.id })
        assertEquals(deepDive.id, result.organismDeepDives.first().id)
    }

    @Test
    fun severeRenalAndAllergySelectionsProduceGuardrails() {
        val result = engine.evaluate(
            TreatmentSelection(
                syndromeEntryId = syndrome.id,
                severity = DecisionSeverity.SEVERE_OR_SHOCK,
                betaLactamAllergy = BetaLactamAllergy.IMMEDIATE_SEVERE,
                kidneyStatus = KidneyStatus.DIALYSIS_OR_RRT,
                hepaticImpairment = true,
            )
        )

        assertTrue(result.notes.any { it.level == DecisionNoteLevel.CRITICAL })
        assertTrue(result.notes.any { it.titleEn.contains("beta-lactam") })
        assertTrue(result.notes.any { it.titleEn.contains("Dialysis") })
        assertTrue(result.notes.any { it.titleEn.contains("hepatic") })
    }

    private fun ReferenceEntry.field(label: String): String =
        fields.first { it.label == label }.value
}
