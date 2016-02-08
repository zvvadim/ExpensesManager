package ua.kiev.bpo.expensesmanager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import ua.kiev.bpo.expensesmanager.ExpensesManagerExpressionEvaluator.EvaluateCallback;

public class ExpensesManager extends Activity implements EvaluateCallback{

    private static final String NAME = ExpensesManager.class.getName();

    // instance state keys
    private static final String KEY_CURRENT_STATE = NAME + "_currentState";
    private static final String KEY_CURRENT_EXPRESSION = NAME + "_currentExpression";

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;

    private enum ExpensesManagerState {INPUT, EVALUATE, RESULT, ERROR};

    private View mDisplayView;
    private ExpensesManagerEditText mFormulaEditText;
    private ExpensesManagerEditText mResultEditText;

    private ExpensesManagerState mCurrentState;
    private ExpensesManagerExpressionTokenizer mTokenizer;
    private ExpensesManagerExpressionEvaluator mEvaluator;

    private Animator mCurrentAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator_port);

        mDisplayView = (View) findViewById(R.id.display);
        mFormulaEditText = (ExpensesManagerEditText) findViewById(R.id.formula);
        mResultEditText = (ExpensesManagerEditText) findViewById(R.id.result);

        mTokenizer = new ExpensesManagerExpressionTokenizer(this);
        mEvaluator = new ExpensesManagerExpressionEvaluator(mTokenizer);

        savedInstanceState = savedInstanceState == null ? Bundle.EMPTY : savedInstanceState;
        setState(ExpensesManagerState.values()[savedInstanceState.getInt(KEY_CURRENT_STATE,
                ExpensesManagerState
                .INPUT.ordinal())]);
    }

    private void setState(ExpensesManagerState state){
        if (mCurrentState != state){
            mCurrentState = state;
        }
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == ExpensesManagerState.INPUT){
            mResultEditText.setText(result);
        } else if (errorResourceId != INVALID_RES_ID){
            onError(errorResourceId);
        } else if (!TextUtils.isEmpty(result)){
            onResult(result);
        } else if (mCurrentState == ExpensesManagerState.EVALUATE){
            setState(ExpensesManagerState.INPUT);
        }

        mFormulaEditText.requestFocus();
    }

    private void onError(final int errorResourceId){
        if (mCurrentState == ExpensesManagerState.EVALUATE){
            mResultEditText.setText(errorResourceId);
            return;
        }
    }

    private void onResult(final String result){
        // Calculate the values needed to perform the scale and translation animations,
        // accounting for how the scale will affect the final position of the text.
        final float resultScale =
                mFormulaEditText.getVariableTextSize(result) / mResultEditText.getTextSize();
        final float resultTranslationX = (1.0f - resultScale) *
                (mResultEditText.getWidth() / 2.0f - mResultEditText.getPaddingEnd());
        final float resultTranslationY = (1.0f - resultScale) *
                (mResultEditText.getHeight() / 2.0f - mResultEditText.getPaddingBottom()) +
                (mFormulaEditText.getBottom() - mResultEditText.getBottom()) +
                (mResultEditText.getPaddingBottom() - mFormulaEditText.getPaddingBottom());
        final float formulaTranslationY = -mFormulaEditText.getBottom();

        // Use a value animator to fade to the final text color over the course of the animation.
        final int resultTextColor = mResultEditText.getCurrentTextColor();
        final int formulaTextColor = mFormulaEditText.getCurrentTextColor();
        final ValueAnimator textColorAnimator =
                ValueAnimator.ofObject(new ArgbEvaluator(), resultTextColor, formulaTextColor);
        textColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mResultEditText.setTextColor((int) valueAnimator.getAnimatedValue());
            }
        });

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                textColorAnimator,
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_X, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_Y, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_X, resultTranslationX),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_Y, resultTranslationY),
                ObjectAnimator.ofFloat(mFormulaEditText, View.TRANSLATION_Y, formulaTranslationY));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mResultEditText.setText(result);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset all of the values modified during the animation.
                mResultEditText.setTextColor(resultTextColor);
                mResultEditText.setScaleX(1.0f);
                mResultEditText.setScaleY(1.0f);
                mResultEditText.setTranslationX(0.0f);
                mResultEditText.setTranslationY(0.0f);
                mFormulaEditText.setTranslationY(0.0f);

                // Finally update the formula to use the current result.
                mFormulaEditText.setText(result);
                setState(ExpensesManagerState.RESULT);

                mCurrentAnimator = null;
            }
        });

        mCurrentAnimator = animatorSet;
        animatorSet.start();
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.eq:
                onEquals();
                break;
            case R.id.del:
                onDelete();
                break;
            case R.id.clr:
                onClear();
                break;
            default:
                mFormulaEditText.append(((Button) view).getText());
                break;
        }
    }

    private void onEquals(){
        if (mCurrentState == ExpensesManagerState.INPUT){
            setState(ExpensesManagerState.EVALUATE);
            mEvaluator.evaluate(mFormulaEditText.getText(),this);
        }
    }

    private void onDelete(){

    }

    private void onClear(){

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before it is serialized.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.end();
        }
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_STATE, mCurrentState.ordinal());
        outState.putString(KEY_CURRENT_EXPRESSION,
                mTokenizer.getNormalizedExpression(mFormulaEditText.getText().toString()));
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before the pending user interaction is handled.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.end();
        }
    }
}
