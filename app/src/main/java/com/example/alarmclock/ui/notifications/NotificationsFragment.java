package com.example.alarmclock.ui.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment; // Import NavController
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Import các lớp và thư viện cần thiết
import com.example.alarmclock.R;
import com.example.alarmclock.databinding.FragmentNotificationsBinding;
import com.example.alarmclock.ui.quiz.QuizAttempt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.alarmclock.alarm.Question; // Cần Question nếu AnsweredQuestion dùng nó

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class NotificationsFragment extends Fragment {

    // Đổi tên binding nếu bạn đổi tên file layout thành fragment_quiz_history.xml
    private FragmentNotificationsBinding binding;
    private QuizHistoryAdapter adapter;
    private List<QuizAttempt> quizHistoryList = new ArrayList<>();
    private static final String TAG = "QuizHistoryFragment";
    private static final String QUIZ_HISTORY_FILENAME = "quiz_history.json";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout mới
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        // binding = FragmentNotificationsBinding.inflate(inflater, container, false); // Nếu không đổi tên layout

        setupRecyclerView();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadHistory(); // Tải dữ liệu khi view đã được tạo
    }

    private void setupRecyclerView() {
        binding.quizHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QuizHistoryAdapter(quizHistoryList, attempt -> {
            // Xử lý sự kiện click vào một item
            Log.d(TAG, "Clicked on attempt: " + new Date(attempt.attemptTimestampStart));
            // Chuyển sang màn hình chi tiết (cần tạo Fragment/Activity mới và cập nhật navigation)
            // Ví dụ:
             Bundle bundle = new Bundle();
             bundle.putSerializable("quizAttempt", attempt); // Truyền đối tượng QuizAttempt
             NavController navController = NavHostFragment.findNavController(NotificationsFragment.this);
             navController.navigate(R.id.action_notifications_to_quizDetailFragment, bundle);
        });
        binding.quizHistoryRecyclerView.setAdapter(adapter);
    }

    private void loadHistory() {
        List<QuizAttempt> loadedHistory = loadQuizHistoryFromFile();
        quizHistoryList.clear();
        if (loadedHistory != null) {
            // Sắp xếp lịch sử để hiển thị cái mới nhất lên đầu (tùy chọn)
            Collections.sort(loadedHistory, (a1, a2) -> Long.compare(a2.attemptTimestampStart, a1.attemptTimestampStart));
            quizHistoryList.addAll(loadedHistory);
        }

        Log.d(TAG, "Loaded " + quizHistoryList.size() + " quiz attempts.");
        adapter.notifyDataSetChanged(); // Cập nhật RecyclerView

        // Hiển thị/ẩn thông báo nếu không có lịch sử
        if (quizHistoryList.isEmpty()) {
            binding.emptyHistoryTextView.setVisibility(View.VISIBLE);
            binding.quizHistoryRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyHistoryTextView.setVisibility(View.GONE);
            binding.quizHistoryRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // --- Copy hàm loadQuizHistoryFromFile từ QuizActivity vào đây ---
    private List<QuizAttempt> loadQuizHistoryFromFile() {
        File file = new File(requireContext().getFilesDir(), QUIZ_HISTORY_FILENAME); // Dùng requireContext()
        if (!file.exists()) {
            return new ArrayList<>();
        }
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) { sb.append(line); }
        } catch (IOException e) {
            Log.e(TAG, "Error reading quiz history file", e);
            return new ArrayList<>();
        }
        String jsonString = sb.toString();
        if (jsonString.isEmpty()) { return new ArrayList<>(); }
        try {
            Type historyListType = new TypeToken<ArrayList<QuizAttempt>>() {}.getType();
            Gson gson = new Gson();
            List<QuizAttempt> history = gson.fromJson(jsonString, historyListType);
            return (history != null) ? history : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing quiz history JSON", e);
            return new ArrayList<>();
        }
    }
    // --- Kết thúc hàm load ---


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Quan trọng để tránh memory leak
    }

    // --- Adapter và ViewHolder cho RecyclerView ---
    private static class QuizHistoryAdapter extends RecyclerView.Adapter<QuizHistoryAdapter.ViewHolder> {

        private final List<QuizAttempt> historyList;
        private final OnItemClickListener listener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());


        interface OnItemClickListener {
            void onItemClick(QuizAttempt attempt);
        }

        QuizHistoryAdapter(List<QuizAttempt> historyList, OnItemClickListener listener) {
            this.historyList = historyList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz_history, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuizAttempt attempt = historyList.get(position);
            holder.bind(attempt, listener);

            holder.subjectDifficultyTextView.setText(attempt.subject + " / " + attempt.difficulty);
            // Format ngày giờ
            Date date = new Date(attempt.attemptTimestampStart);
            holder.dateTimeTextView.setText(timeFormat.format(date) + " " + dateFormat.format(date));
            holder.scoreTextView.setText(attempt.correctAnswers + "/" + attempt.totalQuestionsInThisAttempt);

            // Thay đổi màu nền dựa trên kết quả (ví dụ)
            int backgroundColor = attempt.passed
                    ? holder.itemView.getContext().getColor(R.color.teal_200) // Cần định nghĩa màu này trong colors.xml
                    : holder.itemView.getContext().getColor(R.color.teal_700); // Cần định nghĩa màu này trong colors.xml
            // holder.itemView.setBackgroundColor(backgroundColor); // Có thể đặt màu cho cả card
            // Hoặc bạn có thể muốn đặt màu cho một phần tử cụ thể bên trong
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView subjectDifficultyTextView;
            TextView dateTimeTextView;
            TextView scoreTextView;
            Button viewAnswersButton;

            ViewHolder(View view) {
                super(view);
                subjectDifficultyTextView = view.findViewById(R.id.subjectDifficultyTextView);
                dateTimeTextView = view.findViewById(R.id.dateTimeTextView);
                scoreTextView = view.findViewById(R.id.scoreTextView);
                viewAnswersButton = view.findViewById(R.id.viewAnswersButton); // Lấy ID nút
            }

            void bind(final QuizAttempt attempt, final OnItemClickListener listener) {
                // Đặt listener cho cả item hoặc chỉ cho nút "View Answers"
                viewAnswersButton.setOnClickListener(v -> listener.onItemClick(attempt));
                // Hoặc đặt cho cả item:
                itemView.setOnClickListener(v -> listener.onItemClick(attempt));
            }
        }
    }
}