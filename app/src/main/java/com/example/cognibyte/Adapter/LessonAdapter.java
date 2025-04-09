package com.example.cognibyte.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.R;
import java.util.List;
import models.LessonCompleted;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {

    public interface OnLessonClickListener {
        void onLessonClick(int lessonNumber);
    }

    private List<LessonCompleted> lessonItems;
    private OnLessonClickListener listener;

    public LessonAdapter(List<LessonCompleted> lessonItems, OnLessonClickListener listener) {
        this.lessonItems = lessonItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        LessonCompleted item = lessonItems.get(position);
        holder.btnLesson.setText("Lesson " + item.lessonNumber);
        holder.btnLesson.setEnabled(item.isEnabled);
        holder.btnLesson.setAlpha(item.isEnabled ? 1.0f : 0.5f);
        holder.btnLesson.setOnClickListener(v -> {
            if (item.isEnabled && listener != null) {
                listener.onLessonClick(item.lessonNumber);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lessonItems.size();
    }

    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        Button btnLesson;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            btnLesson = itemView.findViewById(R.id.btnLesson);
        }
    }

    public void updateLessonItems(List<LessonCompleted> items) {
        this.lessonItems = items;
        notifyDataSetChanged();
    }
}
