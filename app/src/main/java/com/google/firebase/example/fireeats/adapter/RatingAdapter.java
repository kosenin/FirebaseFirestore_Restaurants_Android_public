package com.google.firebase.example.fireeats.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.example.fireeats.R;
import com.google.firebase.example.fireeats.model.Rating;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * RecyclerView адаптер для листа с рейтингами
 *
 */
public class RatingAdapter extends FirestoreAdapter<RatingAdapter.ViewHolder> {

    public RatingAdapter(Query query) {
        super(query);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // "Надуваем" UI
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rating, parent, false));
    }

    // Определяем действия при отображении UI
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(getSnapshot(position).toObject(Rating.class));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private static final SimpleDateFormat FORMAT  = new SimpleDateFormat(
                "MM/dd/yyyy", Locale.US);
        // связываем переменные с UI
        @BindView(R.id.rating_item_name)
        TextView nameView;

        @BindView(R.id.rating_item_rating)
        MaterialRatingBar ratingBar;

        @BindView(R.id.rating_item_text)
        TextView textView;

        @BindView(R.id.rating_item_date)
        TextView dateView;

        // ViewHolder для переиспользования имеющихся view
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        // Определяем действия при отображении UI
        public void bind(Rating rating) {
            nameView.setText(rating.getUserName());
            ratingBar.setRating((float) rating.getRating());
            textView.setText(rating.getText());

            if (rating.getTimestamp() != null) {
                dateView.setText(FORMAT.format(rating.getTimestamp()));
            }
        }
    }

}
