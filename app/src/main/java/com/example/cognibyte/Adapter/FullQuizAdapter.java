package com.example.cognibyte.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.R;
import java.util.List;
import models.QuestionItem;

public class FullQuizAdapter extends RecyclerView.Adapter<FullQuizAdapter.QuestionViewHolder> {

    private List<QuestionItem> questionsList;

    public FullQuizAdapter(List<QuestionItem> questionsList) {
        this.questionsList = questionsList;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_full_quiz_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        QuestionItem item = questionsList.get(position);

        holder.tvQuestion.setText((position + 1) + ". " + item.getQuestion());
        holder.radioGroup.removeAllViews();

        for (String option : item.getOptions()) {
            RadioButton radioButton = new RadioButton(holder.itemView.getContext());
            radioButton.setText(option);
            radioButton.setTextSize(14f);
            holder.radioGroup.addView(radioButton);

            if (option.equals(item.getSelectedAnswer())) {
                radioButton.setChecked(true);
            }
        }

        holder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedBtn = group.findViewById(checkedId);
            if (selectedBtn != null) {
                item.setSelectedAnswer(selectedBtn.getText().toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return questionsList.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion;
        RadioGroup radioGroup;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            radioGroup = itemView.findViewById(R.id.radioGroupOptions);
        }
    }
}
