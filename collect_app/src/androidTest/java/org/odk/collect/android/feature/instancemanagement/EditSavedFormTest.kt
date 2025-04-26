package org.odk.collect.android.feature.instancemanagement

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.javarosa.xform.parse.XFormParser
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.odk.collect.android.support.TestDependencies
import org.odk.collect.android.support.pages.FormHierarchyPage
import org.odk.collect.android.support.pages.MainMenuPage
import org.odk.collect.android.support.pages.SendFinalizedFormPage
import org.odk.collect.android.support.rules.CollectTestRule
import org.odk.collect.android.support.rules.RecentAppsRule
import org.odk.collect.android.support.rules.TestRuleChain.chain
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditSavedFormTest {
    private val rule = CollectTestRule()

    val testDependencies: TestDependencies = TestDependencies()
    private val recentAppsRule = RecentAppsRule()

    @get:Rule
    var copyFormChain: RuleChain = chain(testDependencies)
        .around(recentAppsRule)
        .around(rule)

    @Test
    fun sentFormIsNotAvailableForEditsInTheListOfDrafts() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question.xml")
            .startBlankForm("One Question")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()
            .clickOK(SendFinalizedFormPage())
            .pressBack(MainMenuPage())

            .assertNumberOfEditableForms(0)
            .clickDrafts()
            .assertTextDoesNotExist("One Question")

            // Tests that search doesn't change visibility. Move down to lower testing level.
            // (possibly when replacing CursorLoader)
            .clickMenuFilter()
            .searchInBar("One Question".substring(0, 1))
            .assertTextDoesNotExist("One Question")
    }

    @Test
    fun failedToSendFormIsNotAvailableForEditsInTheListOfDrafts() {
        testDependencies.server.alwaysReturnError()

        rule.startAtMainMenu()
            .copyForm("one-question.xml")
            .startBlankForm("One Question")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()
            .clickOK(SendFinalizedFormPage())
            .pressBack(MainMenuPage())

            .assertNumberOfEditableForms(0)
            .clickDrafts()
            .assertTextDoesNotExist("One Question")

            // Tests that search doesn't change visibility. Move down to lower testing level
            // (possibly when replacing CursorLoader)
            .clickMenuFilter()
            .searchInBar("One Question".substring(0, 1))
            .assertTextDoesNotExist("One Question")
    }

    @Test
    fun formsThatDoNotOptInToBeingEditableCannotBeEdited() {
        rule.startAtMainMenu()
            .copyForm("one-question.xml")
            .startBlankForm("One Question")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickOnForm("One Question")
            .assertNonEditableForm()
    }

    @Test
    fun editingAFinalizedForm_createsANewFormAndKeepsTheOriginalOneIntact() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question-editable.xml")
            .startBlankForm("One Question Editable")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .answerQuestion("what is your age", "456")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(2)
            .clickSelectAll()
            .clickSendSelected()

        val originalAnswer = getAnswer(testDependencies.server.submissions[0], "age")
        val updatedAnswer = getAnswer(testDependencies.server.submissions[1], "age")

        assertThat(originalAnswer, equalTo("123"))
        assertThat(updatedAnswer, equalTo("456"))

        val (firstFormInstanceID, firstFormDeprecatedID) = getIds(testDependencies.server.submissions[0])
        val (secondFormInstanceID, secondFormDeprecatedID) = getIds(testDependencies.server.submissions[1])

        assertThat(firstFormDeprecatedID, equalTo(null))
        assertThat(firstFormInstanceID, equalTo(secondFormDeprecatedID))
        assertThat(secondFormInstanceID, not(firstFormInstanceID))
    }

    @Test
    fun editingASentForm_createsANewFormAndKeepsTheOriginalOneIntact() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question-editable.xml")
            .startBlankForm("One Question Editable")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()
            .clickOK(SendFinalizedFormPage())
            .pressBack(MainMenuPage())

            .clickViewSentForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .answerQuestion("what is your age", "456")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()

        val originalAnswer = getAnswer(testDependencies.server.submissions[0], "age")
        val updatedAnswer = getAnswer(testDependencies.server.submissions[1], "age")

        assertThat(originalAnswer, equalTo("123"))
        assertThat(updatedAnswer, equalTo("456"))

        val (firstFormInstanceID, firstFormDeprecatedID) = getIds(testDependencies.server.submissions[0])
        val (secondFormInstanceID, secondFormDeprecatedID) = getIds(testDependencies.server.submissions[1])

        assertThat(firstFormDeprecatedID, equalTo(null))
        assertThat(firstFormInstanceID, equalTo(secondFormDeprecatedID))
        assertThat(secondFormInstanceID, not(firstFormInstanceID))
    }

    @Test
    fun discardingChangesWhenEditingFinalizedForm_createsDraftWithOriginalAnswersAndUpdatesInstanceIdAndDeprecatedId() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question-editable.xml")
            .startBlankForm("One Question Editable")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()
            .clickOK(SendFinalizedFormPage())
            .pressBack(MainMenuPage())

            .clickViewSentForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .answerQuestion("what is your age", "456")
            .pressBackAndDiscardChanges()

            .clickDrafts(1)
            .clickOnForm("One Question Editable")
            .assertText("123")
            .clickGoToEnd()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()

        val (firstFormInstanceID, firstFormDeprecatedID) = getIds(testDependencies.server.submissions[0])
        val (secondFormInstanceID, secondFormDeprecatedID) = getIds(testDependencies.server.submissions[1])

        assertThat(firstFormDeprecatedID, equalTo(null))
        assertThat(firstFormInstanceID, equalTo(secondFormDeprecatedID))
        assertThat(secondFormInstanceID, not(firstFormInstanceID))
    }

    @Test
    fun killingAppWhenEditingFinalizedForm_createsSavepointForFormRecovery() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question-editable.xml")
            .startBlankForm("One Question Editable")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .answerQuestion("what is your age", "456")
            .killAndReopenApp(rule, recentAppsRule, MainMenuPage(), testDependencies)

            .clickDrafts(1)
            .clickOnFormWithSavepoint("One Question Editable")
            .clickRecover(FormHierarchyPage("One Question Editable"))
            .assertText("456")
            .clickGoToEnd()
            .clickFinalize()

            .clickSendFinalizedForm(2)
            .clickSelectAll()
            .clickSendSelected()

        val (firstFormInstanceID, firstFormDeprecatedID) = getIds(testDependencies.server.submissions[0])
        val (secondFormInstanceID, secondFormDeprecatedID) = getIds(testDependencies.server.submissions[1])

        assertThat(firstFormDeprecatedID, equalTo(null))
        assertThat(firstFormInstanceID, equalTo(secondFormDeprecatedID))
        assertThat(secondFormInstanceID, not(firstFormInstanceID))
    }

    @Test
    fun editingAnEditedForm_updatesInstanceIdAndDeprecatedId() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question-editable.xml")
            .startBlankForm("One Question Editable")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .pressBackAndSaveAsDraft()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()
            .clickOK(SendFinalizedFormPage())
            .pressBack(MainMenuPage())

            .clickDrafts(1)
            .clickOnForm("One Question Editable")
            .clickGoToEnd()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(2)
            .clickSelectAll()
            .clickSendSelected()

        val (firstFormInstanceID, firstFormDeprecatedID) = getIds(testDependencies.server.submissions[0])
        val (secondFormInstanceID, secondFormDeprecatedID) = getIds(testDependencies.server.submissions[1])
        val (thirdFormInstanceID, thirdFormDeprecatedID) = getIds(testDependencies.server.submissions[2])

        assertThat(firstFormDeprecatedID, equalTo(null))
        assertThat(firstFormInstanceID, equalTo(secondFormDeprecatedID))
        assertThat(secondFormInstanceID, not(firstFormInstanceID))
        assertThat(secondFormInstanceID, equalTo(thirdFormDeprecatedID))
        assertThat(thirdFormInstanceID, not(secondFormInstanceID))
    }

    @Test
    fun savingEditedFormMultipleTimes_preservesDeprecatedId() {
        rule.startAtMainMenu()
            .setServer(testDependencies.server.url)
            .copyForm("one-question-editable.xml")
            .startBlankForm("One Question Editable")
            .answerQuestion("what is your age", "123")
            .swipeToEndScreen()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()
            .clickOK(SendFinalizedFormPage())
            .pressBack(MainMenuPage())

            .clickViewSentForm(1)
            .clickOnForm("One Question Editable")
            .editForm("One Question Editable")
            .clickOnQuestion("what is your age")
            .answerQuestion("what is your age", "456")
            .pressBackAndSaveAsDraft()

            .clickDrafts(1)
            .clickOnForm("One Question Editable")
            .assertText("456")
            .clickGoToEnd()
            .clickFinalize()

            .clickSendFinalizedForm(1)
            .clickSelectAll()
            .clickSendSelected()

        val (firstFormInstanceID, firstFormDeprecatedID) = getIds(testDependencies.server.submissions[0])
        val (secondFormInstanceID, secondFormDeprecatedID) = getIds(testDependencies.server.submissions[1])

        assertThat(firstFormDeprecatedID, equalTo(null))
        assertThat(firstFormInstanceID, equalTo(secondFormDeprecatedID))
        assertThat(secondFormInstanceID, not(firstFormInstanceID))
    }

    private fun getIds(file: File): Pair<String, String?> {
        val formRootElement = XFormParser.getXMLDocument(file.inputStream().reader()).rootElement
        val formMetaElement = formRootElement.getElement(null, "meta")
        val instanceID = formMetaElement.getElement(null, "instanceID").getText(0)
        val deprecatedID = try {
            formMetaElement.getElement(null, "deprecatedID").getText(0)
        } catch (e: Exception) {
            null
        }

        return Pair(instanceID, deprecatedID)
    }

    private fun getAnswer(file: File, questionName: String): String? {
        val formRootElement = XFormParser.getXMLDocument(file.inputStream().reader()).rootElement

        return formRootElement.getElement(null, questionName).getText(0)
    }
}
