package com.example.cognibyte.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cognibyte.R;

import java.util.List;

public class CodeQuizAdapter extends RecyclerView.Adapter<CodeQuizAdapter.ViewHolder> {
    private List<String> quizQuestions;

    public CodeQuizAdapter(List<String> quizQuestions) {
        this.quizQuestions = quizQuestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_code_quiz, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String question = quizQuestions.get(position);
        holder.tvQuizQuestion.setText(question);
    }

    @Override
    public int getItemCount() {
        return quizQuestions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuizQuestion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuizQuestion = itemView.findViewById(R.id.tvQuizQuestion);
        }
    }
}
