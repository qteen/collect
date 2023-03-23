package org.odk.collect.android.utilities;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

public class ClickSpan extends ClickableSpan {

	private boolean withUnderline;
	private String path;
	private OnClickListener listener;

	public interface OnClickListener {
		void onSpanClicked(String path);
	}

	//region ==================== Static ====================

	public static void clickify(TextView view, final String clickableText, final String path,
								final OnClickListener listener) {
		clickify(view, clickableText, path, true, listener);
	}

	public static void clickify(TextView view, final String clickableText, final String path,
	                            boolean withUnderline,
	                            final OnClickListener listener) {

		CharSequence text = view.getText();
		String string = text.toString();
		ClickSpan span = new ClickSpan(withUnderline, path, listener);

		int start = string.indexOf(clickableText);
		int end = start + clickableText.length();
		if (start == -1) {
			return;
		}

		if (text instanceof Spannable) {
			((Spannable) text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			SpannableString s = SpannableString.valueOf(text);
			s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			view.setText(s);
		}

		MovementMethod m = view.getMovementMethod();
		if (!(m instanceof LinkMovementMethod)) {
			view.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	//endregion

	public ClickSpan(boolean withUnderline, String path, OnClickListener listener) {
		this.withUnderline = withUnderline;
		this.path = path;
		this.listener = listener;
	}

	//region ==================== Override ====================

	@Override
	public void onClick(View widget) {
		if (listener != null) {
			listener.onSpanClicked(path);
		}
	}

	@Override
	public void updateDrawState(TextPaint paint) {
		super.updateDrawState(paint);
		paint.setUnderlineText(withUnderline);
	}

}
