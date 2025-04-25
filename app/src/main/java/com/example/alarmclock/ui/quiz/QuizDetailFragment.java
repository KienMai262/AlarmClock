package com.example.alarmclock.ui.quiz; // Hoặc package phù hợp

import android.annotation.SuppressLint;
import android.content.Context; // Import Context
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarmclock.R;
import com.example.alarmclock.ui.quiz.AnsweredQuestion; // Giả sử đã chuyển vào data
import com.example.alarmclock.ui.quiz.QuizAttempt;    // Giả sử đã chuyển vào data
import com.example.alarmclock.databinding.FragmentQuizDetailBinding;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizDetailFragment extends Fragment {

    private FragmentQuizDetailBinding binding;
    private QuizAttempt quizAttempt;
    private QuizDetailAdapter adapter;
    private static final String TAG = "QuizDetailFragment";

    // Bỏ các biến label khởi tạo ở đây

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quizAttempt = (QuizAttempt) getArguments().getSerializable("quizAttempt");
        }
        if (quizAttempt == null) {
            Log.e(TAG, "QuizAttempt data is null!");
            // Nên lấy context ở đây nếu cần Toast, hoặc tốt nhất là không Toast trong onCreate
            // Toast.makeText(getContext(), "Lỗi dữ liệu chi tiết quiz.", Toast.LENGTH_SHORT).show();
            // Cẩn thận khi gọi navigate ở đây, fragment có thể chưa sẵn sàng
            if(isAdded()) { // Chỉ navigate nếu fragment đã được thêm vào Activity
                NavHostFragment.findNavController(this).popBackStack();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuizDetailBinding.inflate(inflater, container, false);
        return binding.getRoot(); // Trả về root view
    }

    // Di chuyển logic vào onViewCreated là an toàn nhất
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Chỉ thực hiện nếu quizAttempt hợp lệ
        if (quizAttempt != null) {
            setupToolbar();
            populateSummary(); // Lấy string bên trong hàm này
            setupRecyclerView(); // Tạo adapter sau khi đã lấy được context
        } else {
            // Xử lý nếu quizAttempt vẫn null (ví dụ: quay lại màn hình trước)
            Log.e(TAG, "quizAttempt is null in onViewCreated, navigating back.");
            if(isAdded()) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        }
    }


    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void populateSummary() {
        // Lấy các label string ở đây, khi Context đã chắc chắn có
        String difficultyLabel = getString(R.string.difficulty);
        String completeLabel = getString(R.string.complete); // Sửa typo
        String scoreLabel = getString(R.string.score);

        binding.detailSubjectTopicTextView.setText(quizAttempt.subject + " - " + quizAttempt.topic);
        binding.detailDifficultyTextView.setText(difficultyLabel + ": " + quizAttempt.difficulty);

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM, yyyy h:mm a", Locale.getDefault());
        binding.detailTimestampTextView.setText(completeLabel + ": " + dateTimeFormat.format(new Date(quizAttempt.attemptTimestampEnd)));

        double scorePercent = quizAttempt.getScorePercentage();
        binding.detailScoreTextView.setText(String.format(Locale.getDefault(), "%s: %d/%d (%.1f%%)",
                scoreLabel,
                quizAttempt.correctAnswers,
                quizAttempt.totalQuestionsInThisAttempt,
                scorePercent));

        binding.detailResultTextView.setText(quizAttempt.passed ? R.string.pass : R.string.fail);
        binding.detailResultTextView.setTextColor(quizAttempt.passed ? Color.parseColor("#4CAF50") : Color.RED); // Dùng parseColor nếu muốn màu chính xác
    }

    private void setupRecyclerView() {
        if (quizAttempt.answeredQuestions == null) {
            binding.detailRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Lấy các string cần thiết cho Adapter ngay tại đây
        String questionLabel = getString(R.string.question);
        String youChooseLabel = getString(R.string.you_choose);
        String timeoutLabel = getString(R.string.time_out);
        String correctAnswerLabel = getString(R.string.the_correct_answer);


        binding.detailRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Truyền các string đã lấy vào constructor của Adapter
        adapter = new QuizDetailAdapter(quizAttempt.answeredQuestions, questionLabel, youChooseLabel, timeoutLabel, correctAnswerLabel);
        binding.detailRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Adapter và ViewHolder cho RecyclerView chi tiết ---
    // Bỏ static để có thể truy cập gián tiếp Context nếu thật sự cần,
    // NHƯNG tốt hơn là truyền tài nguyên qua constructor
    private static class QuizDetailAdapter extends RecyclerView.Adapter<QuizDetailAdapter.ViewHolder> {

        private final List<AnsweredQuestion> answeredQuestions;
        // Thêm các biến để giữ các chuỗi tài nguyên
        private final String questionLabel;
        private final String youChooseLabel;
        private final String timeoutLabel;
        private final String correctAnswerLabel;

        // Sửa constructor để nhận các chuỗi
        QuizDetailAdapter(List<AnsweredQuestion> items, String questionLabel, String youChooseLabel, String timeoutLabel, String correctAnswerLabel) {
            this.answeredQuestions = items != null ? items : Collections.emptyList();
            this.questionLabel = questionLabel;
            this.youChooseLabel = youChooseLabel;
            this.timeoutLabel = timeoutLabel;
            this.correctAnswerLabel = correctAnswerLabel;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz_detail_answer, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AnsweredQuestion answered = answeredQuestions.get(position);
            if (answered == null) return;

            // Sử dụng các chuỗi đã được truyền vào
            holder.questionNumberTextView.setText(String.format(Locale.getDefault(),"%s %d:", questionLabel, position + 1));
            holder.questionContentTextView.setText(answered.questionContent != null ? answered.questionContent : "N/A");

            String userAnswerText = (answered.userAnswer != null && !answered.userAnswer.isEmpty()) ? answered.userAnswer : "(Không trả lời)";
            if ("[TIMEOUT]".equals(answered.userAnswer)) {
                userAnswerText = "( " + timeoutLabel + " )"; // Dùng biến timeoutLabel
            }

            holder.userAnswerTextView.setText(youChooseLabel + ": " + userAnswerText); // Dùng biến youChooseLabel

            if (answered.wasCorrect) {
                holder.userAnswerTextView.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.userAnswerTextView.setTextColor(Color.RED);
            }

            if (answered.trueAnswer != null) {
                holder.correctAnswerTextView.setText(correctAnswerLabel + ": " + answered.trueAnswer); // Dùng biến correctAnswerLabel
                holder.correctAnswerTextView.setVisibility(View.VISIBLE);
            } else {
                holder.correctAnswerTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return answeredQuestions.size();
        }

        // ViewHolder giữ nguyên
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView questionNumberTextView;
            TextView questionContentTextView;
            TextView userAnswerTextView;
            TextView correctAnswerTextView;

            ViewHolder(View view) {
                super(view);
                questionNumberTextView = view.findViewById(R.id.questionNumberTextView);
                questionContentTextView = view.findViewById(R.id.questionContentTextView);
                userAnswerTextView = view.findViewById(R.id.userAnswerTextView);
                correctAnswerTextView = view.findViewById(R.id.correctAnswerTextView);
            }
        }
    }
}