package org.odk.collect.android.widgets.support;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.fakes.FakePermissionUtils;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.support.RobolectricHelpers;
import org.odk.collect.android.support.TestScreenContextActivity;
import org.odk.collect.android.widgets.QuestionWidget;

import static org.mockito.Mockito.mock;

public class QuestionWidgetHelpers {

    private QuestionWidgetHelpers() {

    }

    public static TestScreenContextActivity widgetTestActivity() {
        return RobolectricHelpers.buildThemedActivity(TestScreenContextActivity.class).get();
    }

    public static <T extends QuestionWidget> WidgetValueChangedListener mockValueChangedListener(T widget) {
        WidgetValueChangedListener valueChangedListener = mock(WidgetValueChangedListener.class);
        widget.setValueChangedListener(valueChangedListener);
        return valueChangedListener;
    }

    public static void stubLocationPermissions(FakePermissionUtils permissionUtils, QuestionWidget widget, boolean isGranted) {
        permissionUtils.setPermissionGranted(isGranted);
        widget.setPermissionUtils(permissionUtils);
    }

    public static FormEntryPrompt promptWithAnswer(IAnswerData answer) {
        return new MockFormEntryPromptBuilder()
                .withAnswer(answer)
                .build();
    }

    public static FormEntryPrompt promptWithReadOnly() {
        return new MockFormEntryPromptBuilder()
                .withReadOnly(true)
                .build();
    }

    public static FormEntryPrompt promptWithReadOnlyAndAnswer(IAnswerData answer) {
        return new MockFormEntryPromptBuilder()
                .withReadOnly(true)
                .withAnswer(answer)
                .build();
    }

    public static FormEntryPrompt promptWithQuestionDefAndAnswer(QuestionDef questionDef, IAnswerData answer) {
        return new MockFormEntryPromptBuilder()
                .withQuestion(questionDef)
                .withAnswer(answer)
                .build();
    }

    public static FormEntryPrompt promptWithAppearance(String appearance) {
        return new MockFormEntryPromptBuilder()
                .withAppearance(appearance)
                .build();
    }

    public static FormEntryPrompt promptWithReadOnlyAndQuestionDef(QuestionDef questionDef) {
        return new MockFormEntryPromptBuilder()
                .withReadOnly(true)
                .withQuestion(questionDef)
                .build();
    }
}
