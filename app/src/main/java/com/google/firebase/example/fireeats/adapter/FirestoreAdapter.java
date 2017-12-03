package com.google.firebase.example.fireeats.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

/**
 * Абстрактный RecyclerView адаптер
 *
 * Класс является абстракным, его наследуют RatingAdapter и RestaurantAdapter и используют его методы
 *
 * Заметьте что этот класс упрощен для понимания. Например, результат {@link DocumentSnapshot#toObject(Class)}
 * не закэширован поэтому один и тот же объект может быть десереализирован много раз в то время как пользователь скроллит
 */
public abstract class FirestoreAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements EventListener<QuerySnapshot> {

    private static final String TAG = "FirestoreAdapter";
    // Переменные
    // Запрос
    private Query mQuery;
    // Подписка
    private ListenerRegistration mRegistration;
    // Лист
    private ArrayList<DocumentSnapshot> mSnapshots = new ArrayList<>();
    // Конструктор
    public FirestoreAdapter(Query query) {
        mQuery = query;
    }

    // переписанный метод интерфейса EventListener
    @Override
    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
        if (e != null) {
            Log.w(TAG, "onEvent: ошибка", e);
            onError(e);
            return;
        }

        // Запускаем event в зависимости от изменения, произошедшего в Firestore
        Log.d(TAG, "onEvent:numChanges:" + documentSnapshots.getDocumentChanges().size());
        for (DocumentChange change : documentSnapshots.getDocumentChanges()) {
            switch (change.getType()) {
                case ADDED:// добавление
                    onDocumentAdded(change);
                    break;
                case MODIFIED:// изменение
                    onDocumentModified(change);
                    break;
                case REMOVED:// удаление
                    onDocumentRemoved(change);
                    break;
            }
        }

        onDataChanged();
    }

    // Начинаем слушать на запросе в Firestore
    public void startListening() {
        if (mQuery != null && mRegistration == null) {
            mRegistration = mQuery.addSnapshotListener(this);
        }
    }

    // Прекращаем слушать
    public void stopListening() {
        if (mRegistration != null) {
            mRegistration.remove();
            mRegistration = null;
        }

        mSnapshots.clear();
        notifyDataSetChanged();
    }

    // Устанавливаем запрос
    public void setQuery(Query query) {
        // Прекращаем слушать
        stopListening();

        // Удаляем существующие данные
        mSnapshots.clear();
        notifyDataSetChanged();

        // Устанавливаем новый запрос
        mQuery = query;
        startListening();
    }

    @Override
    public int getItemCount() {
        return mSnapshots.size();
    }

    protected DocumentSnapshot getSnapshot(int index) {
        return mSnapshots.get(index);
    }

    // данные добавлены
    protected void onDocumentAdded(DocumentChange change) {
        mSnapshots.add(change.getNewIndex(), change.getDocument());
        notifyItemInserted(change.getNewIndex());
    }

    // данные изменены
    protected void onDocumentModified(DocumentChange change) {
        if (change.getOldIndex() == change.getNewIndex()) {
            // Item changed but remained in same position
            mSnapshots.set(change.getOldIndex(), change.getDocument());
            notifyItemChanged(change.getOldIndex());
        } else {
            // Item changed and changed position
            mSnapshots.remove(change.getOldIndex());
            mSnapshots.add(change.getNewIndex(), change.getDocument());
            notifyItemMoved(change.getOldIndex(), change.getNewIndex());
        }
    }

    // данные удалены
    protected void onDocumentRemoved(DocumentChange change) {
        mSnapshots.remove(change.getOldIndex());
        notifyItemRemoved(change.getOldIndex());
    }

    // ошибка
    protected void onError(FirebaseFirestoreException e) {};

    // метод имплементируется (определяется) наследуемымм классами
    protected void onDataChanged() {}
}
