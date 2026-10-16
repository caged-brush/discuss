package com.example.myapplication;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A group or direct chat: text, shared articles, photos and voice messages. */
public class GroupChatActivity extends AppCompatActivity {
    private static final String EXTRA_GROUP_ID = "group_id";
    private static final String STATE_PENDING_PHOTO = "pending_photo";
    private static final String PAYLOAD_AUDIO = "audio";
    private static final long MIN_RECORDING_MS = 700;
    private static final int MENU_TAKE_PHOTO = 1;
    private static final int MENU_CHOOSE_PHOTO = 2;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private DBHandler db;
    private String uid;
    private String groupId;
    private ChatGroup group;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private TextView tvSubtitle;
    private EditText etMessage;
    private ImageButton btnAction;
    private View composerNormal;
    private View composerRecording;
    private TextView tvRecordTimer;

    // Photo being taken by the camera app; kept across process death
    private File pendingPhoto;

    // Voice recording
    private MediaRecorder recorder;
    private File recordFile;
    private long recordStartedAt;
    private final Runnable recordTick = new Runnable() {
        @Override
        public void run() {
            long elapsed = SystemClock.elapsedRealtime() - recordStartedAt;
            tvRecordTimer.setText(MediaFiles.formatDuration(elapsed));
            handler.postDelayed(this, 250);
        }
    };

    // Voice playback
    private MediaPlayer player;
    private long playingMessageId = -1;
    private final Runnable playbackTick = new Runnable() {
        @Override
        public void run() {
            if (player == null) {
                return;
            }
            refreshAudioRow(playingMessageId);
            if (player.isPlaying()) {
                handler.postDelayed(this, 150);
            }
        }
    };

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), this::onPhotoCaptured);
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::onPhotoPicked);
    private final ActivityResultLauncher<String> requestMicrophone = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecording();
                } else {
                    toast(R.string.chat_voice_permission);
                }
            });
    private final ActivityResultLauncher<Intent> dictation = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onDictationResult);

    public static Intent intent(Context context, String groupId) {
        return new Intent(context, GroupChatActivity.class).putExtra(EXTRA_GROUP_ID, groupId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        db = DBHandler.getInstance(this);
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        group = groupId == null ? null : db.getGroup(groupId, uid);
        if (group == null || !db.isGroupMember(groupId, uid)) {
            Toast.makeText(this, R.string.chat_not_member, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (savedInstanceState != null) {
            String path = savedInstanceState.getString(STATE_PENDING_PHOTO);
            if (path != null) {
                pendingPhoto = new File(path);
            }
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Include the keyboard so the composer stays visible while typing
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ((TextView) findViewById(R.id.tvChatTitle)).setText(group.getName());
        Ui.bindAvatar(findViewById(R.id.tvChatAvatar),
                group.isDirect() && group.getPeerUid() != null ? group.getPeerUid() : group.getId(),
                group.getName());
        tvSubtitle = findViewById(R.id.tvChatSubtitle);
        findViewById(R.id.btnChatBack).setOnClickListener(v -> finish());
        findViewById(R.id.chatHeader).setOnClickListener(v -> showGroupOptions());
        findViewById(R.id.btnChatMore).setOnClickListener(v -> showGroupOptions());

        rvMessages = findViewById(R.id.rvChatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        adapter = new MessageAdapter();
        rvMessages.setAdapter(adapter);

        etMessage = findViewById(R.id.etMessage);
        btnAction = findViewById(R.id.btnSendMessage);
        composerNormal = findViewById(R.id.composerNormal);
        composerRecording = findViewById(R.id.composerRecording);
        tvRecordTimer = findViewById(R.id.tvRecordTimer);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                updateActionButton();
            }
        });
        updateActionButton();
        btnAction.setOnClickListener(v -> onActionClicked());
        findViewById(R.id.btnDictate).setOnClickListener(v -> startDictation());
        findViewById(R.id.btnAttach).setOnClickListener(this::showAttachMenu);
        findViewById(R.id.btnRecordCancel).setOnClickListener(v -> stopRecording(false));
        findViewById(R.id.btnRecordSend).setOnClickListener(v -> stopRecording(true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadMessages();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Do not keep the microphone or the speaker busy in the background
        if (recorder != null) {
            stopRecording(false);
        }
        stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdown();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingPhoto != null) {
            outState.putString(STATE_PENDING_PHOTO, pendingPhoto.getAbsolutePath());
        }
    }

    // ------------------------------------------------------------------ messages

    private void toast(int textRes) {
        Toast.makeText(this, textRes, Toast.LENGTH_SHORT).show();
    }

    private void loadMessages() {
        if (isDestroyed() || adapter == null) {
            return;
        }
        if (group.isDirect()) {
            tvSubtitle.setText(R.string.chat_direct);
        } else {
            int members = db.getGroupMembers(groupId).size();
            tvSubtitle.setText(getResources().getQuantityString(R.plurals.groups_members,
                    members, members));
        }
        adapter.setMessages(db.getMessages(groupId, uid));
        if (adapter.getItemCount() > 0) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void updateActionButton() {
        boolean hasText = !etMessage.getText().toString().trim().isEmpty();
        btnAction.setImageResource(hasText ? R.drawable.ic_send : R.drawable.ic_mic);
        btnAction.setContentDescription(getString(hasText
                ? R.string.chat_send : R.string.chat_voice_record));
    }

    private void onActionClicked() {
        String text = etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            db.sendMessage(groupId, uid, text, null);
            etMessage.setText("");
            loadMessages();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            requestMicrophone.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    // -------------------------------------------------------------------- photos

    private void showAttachMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, MENU_TAKE_PHOTO, 0, R.string.chat_take_photo);
        popup.getMenu().add(0, MENU_CHOOSE_PHOTO, 1, R.string.chat_choose_photo);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_TAKE_PHOTO) {
                takePhoto();
            } else {
                pickPhoto.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
            return true;
        });
        popup.show();
    }

    private void takePhoto() {
        File raw = MediaFiles.newFile(MediaFiles.chatDir(this), "cam_", ".jpg");
        try {
            //noinspection ResultOfMethodCallIgnored
            raw.createNewFile();
        } catch (IOException e) {
            toast(R.string.chat_photo_failed);
            return;
        }
        pendingPhoto = raw;
        try {
            takePicture.launch(MediaFiles.uriFor(this, raw));
        } catch (ActivityNotFoundException e) {
            //noinspection ResultOfMethodCallIgnored
            raw.delete();
            pendingPhoto = null;
            toast(R.string.chat_camera_unavailable);
        }
    }

    private void onPhotoCaptured(boolean saved) {
        File raw = pendingPhoto;
        pendingPhoto = null;
        if (raw == null) {
            return;
        }
        if (!saved || raw.length() == 0) {
            //noinspection ResultOfMethodCallIgnored
            raw.delete();
            return;
        }
        sendPhoto(() -> new FileInputStream(raw), raw);
    }

    private void onPhotoPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        sendPhoto(() -> {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) {
                throw new FileNotFoundException(uri.toString());
            }
            return in;
        }, null);
    }

    /** Shrinks and stores the photo off the UI thread, then posts it to the chat. */
    private void sendPhoto(MediaFiles.StreamSource source, File temporaryFile) {
        worker.execute(() -> {
            try {
                String name = MediaFiles.saveChatImage(this, source);
                db.sendMediaMessage(groupId, uid, ChatMessage.MEDIA_IMAGE, name, 0);
                handler.post(this::loadMessages);
            } catch (IOException | RuntimeException e) {
                handler.post(() -> toast(R.string.chat_photo_failed));
            } finally {
                if (temporaryFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    temporaryFile.delete();
                }
            }
        });
    }

    // ----------------------------------------------------------------- dictation

    private void startDictation() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.chat_dictate_prompt));
        try {
            dictation.launch(intent);
        } catch (ActivityNotFoundException e) {
            toast(R.string.chat_dictate_unavailable);
        }
    }

    private void onDictationResult(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() != RESULT_OK || data == null) {
            return;
        }
        ArrayList<String> texts = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (texts == null || texts.isEmpty()) {
            return;
        }
        Editable text = etMessage.getText();
        if (text.length() > 0 && !Character.isWhitespace(text.charAt(text.length() - 1))) {
            text.append(' ');
        }
        text.append(texts.get(0));
        etMessage.setSelection(text.length());
        etMessage.requestFocus();
    }

    // ----------------------------------------------------------- voice recording

    @SuppressWarnings("deprecation")
    private MediaRecorder newRecorder() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? new MediaRecorder(this) : new MediaRecorder();
    }

    private void startRecording() {
        stopPlayback();
        File file = MediaFiles.newFile(MediaFiles.chatDir(this), "voice_", ".m4a");
        MediaRecorder r = newRecorder();
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC);
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            r.setAudioEncodingBitRate(64_000);
            r.setAudioSamplingRate(44_100);
            r.setOutputFile(file.getAbsolutePath());
            r.prepare();
            r.start();
        } catch (IOException | RuntimeException e) {
            r.release();
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            toast(R.string.chat_voice_failed);
            return;
        }
        recorder = r;
        recordFile = file;
        recordStartedAt = SystemClock.elapsedRealtime();
        composerNormal.setVisibility(View.GONE);
        composerRecording.setVisibility(View.VISIBLE);
        handler.post(recordTick);
    }

    /** Finishes the recording; when send is false the audio is thrown away. */
    private void stopRecording(boolean send) {
        if (recorder == null) {
            return;
        }
        handler.removeCallbacks(recordTick);
        long elapsed = SystemClock.elapsedRealtime() - recordStartedAt;
        boolean valid = true;
        try {
            recorder.stop();
        } catch (RuntimeException e) {
            // Thrown when no audio was captured, for example a tap that was too short
            valid = false;
        }
        recorder.release();
        recorder = null;
        File file = recordFile;
        recordFile = null;
        composerRecording.setVisibility(View.GONE);
        composerNormal.setVisibility(View.VISIBLE);
        tvRecordTimer.setText(MediaFiles.formatDuration(0));

        if (!send) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return;
        }
        if (!valid || elapsed < MIN_RECORDING_MS) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            toast(R.string.chat_voice_too_short);
            return;
        }
        long duration = MediaFiles.durationOf(file);
        db.sendMediaMessage(groupId, uid, ChatMessage.MEDIA_AUDIO, file.getName(),
                duration > 0 ? duration : elapsed);
        loadMessages();
    }

    // ------------------------------------------------------------ voice playback

    private void toggleAudio(ChatMessage message) {
        if (playingMessageId == message.getId() && player != null) {
            if (player.isPlaying()) {
                player.pause();
                handler.removeCallbacks(playbackTick);
            } else {
                player.start();
                handler.post(playbackTick);
            }
            refreshAudioRow(message.getId());
            return;
        }
        stopPlayback();
        MediaPlayer p = new MediaPlayer();
        try {
            p.setDataSource(MediaFiles.chatFile(this, message.getMediaPath()).getAbsolutePath());
            p.prepare();
        } catch (IOException | RuntimeException e) {
            p.release();
            toast(R.string.chat_voice_failed);
            return;
        }
        p.setOnCompletionListener(mp -> stopPlayback());
        p.start();
        player = p;
        playingMessageId = message.getId();
        handler.post(playbackTick);
        refreshAudioRow(message.getId());
    }

    private void stopPlayback() {
        handler.removeCallbacks(playbackTick);
        long previous = playingMessageId;
        playingMessageId = -1;
        if (player != null) {
            player.release();
            player = null;
        }
        if (previous != -1) {
            refreshAudioRow(previous);
        }
    }

    private void refreshAudioRow(long messageId) {
        int index = adapter == null ? -1 : adapter.indexOf(messageId);
        if (index >= 0) {
            adapter.notifyItemChanged(index, PAYLOAD_AUDIO);
        }
    }

    // ------------------------------------------------------------ group options

    private void showGroupOptions() {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (group.isDirect()) {
            if (group.getPeerUid() != null) {
                labels.add(getString(R.string.chat_view_profile));
                actions.add(() -> startActivity(ProfileActivity.intent(this, group.getPeerUid())));
            }
            labels.add(getString(R.string.chat_leave_direct));
        } else {
            labels.add(getString(R.string.chat_members_title)
                    + " (" + db.getGroupMembers(groupId).size() + ")");
            actions.add(this::showMembers);
            labels.add(getString(R.string.chat_add_member));
            actions.add(this::showAddMember);
            if (group.getArticleId() != null) {
                labels.add(getString(R.string.chat_open_article));
                actions.add(() -> startActivity(ArticleView.intent(this, group.getArticleId())));
            }
            labels.add(getString(R.string.chat_leave));
        }
        actions.add(() -> {
            db.leaveGroup(groupId, uid);
            finish();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(group.getName())
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void showMembers() {
        List<UserProfile> members = db.getGroupMembers(groupId);
        String[] names = new String[members.size()];
        for (int i = 0; i < members.size(); i++) {
            names[i] = members.get(i).getDisplayName() + "  @" + members.get(i).getUsername();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.chat_members_title)
                .setItems(names, (d, which) ->
                        startActivity(ProfileActivity.intent(this, members.get(which).getUid())))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showAddMember() {
        List<UserProfile> candidates = new ArrayList<>();
        for (UserProfile contact : db.getContacts(uid)) {
            if (!db.isGroupMember(groupId, contact.getUid())) {
                candidates.add(contact);
            }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.chat_no_contacts_to_add, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).getDisplayName();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.chat_add_member)
                .setItems(names, (d, which) -> {
                    db.addMemberToGroup(groupId, candidates.get(which).getUid());
                    Toast.makeText(this, getString(R.string.contacts_added, names[which]),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ------------------------------------------------------------------- adapter

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private final List<ChatMessage> messages = new ArrayList<>();

        void setMessages(List<ChatMessage> newMessages) {
            messages.clear();
            messages.addAll(newMessages);
            notifyDataSetChanged();
        }

        int indexOf(long messageId) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId() == messageId) {
                    return i;
                }
            }
            return -1;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MessageViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position,
                                     @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                // Only the playback position changed
                bindAudioState(holder, messages.get(position));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            boolean mine = message.getSender().getUid().equals(uid);
            View bubble = holder.bubble;

            // Own messages sit on the right in the brand colour, without avatar or name
            int background = MaterialColors.getColor(bubble, mine
                    ? androidx.appcompat.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorSurfaceContainerHigh);
            int foreground = MaterialColors.getColor(bubble, mine
                    ? com.google.android.material.R.attr.colorOnPrimary
                    : com.google.android.material.R.attr.colorOnSurface);
            int accent = mine ? foreground : MaterialColors.getColor(bubble,
                    androidx.appcompat.R.attr.colorPrimary);
            holder.row.setGravity(mine ? Gravity.END : Gravity.START);
            holder.bubble.setCardBackgroundColor(background);

            float big = Ui.dp(GroupChatActivity.this, 18);
            float small = Ui.dp(GroupChatActivity.this, 5);
            ShapeAppearanceModel.Builder shape = holder.bubble.getShapeAppearanceModel()
                    .toBuilder().setAllCornerSizes(big);
            if (mine) {
                shape.setBottomRightCornerSize(small);
            } else {
                shape.setBottomLeftCornerSize(small);
            }
            holder.bubble.setShapeAppearanceModel(shape.build());

            holder.avatar.setVisibility(mine ? View.GONE : View.VISIBLE);
            holder.sender.setVisibility(mine ? View.GONE : View.VISIBLE);
            if (!mine) {
                Ui.bindAvatar(holder.avatar, message.getSender().getUid(),
                        message.getSender().getDisplayName());
                holder.avatar.setOnClickListener(v -> startActivity(ProfileActivity.intent(
                        GroupChatActivity.this, message.getSender().getUid())));
                holder.sender.setText(message.getSender().getDisplayName());
                holder.sender.setTextColor(accent);
            }

            boolean hasText = !message.getBody().isEmpty();
            holder.body.setVisibility(hasText ? View.VISIBLE : View.GONE);
            holder.body.setText(message.getBody());
            holder.body.setTextColor(foreground);

            bindArticle(holder, message, foreground, accent);
            bindPhoto(holder, message);
            bindAudio(holder, message, foreground, background);

            holder.time.setText(Ui.timeOfDay(GroupChatActivity.this, message.getSentAt()));
            holder.time.setTextColor(ColorUtils.setAlphaComponent(foreground, 0xB3));
        }

        private void bindArticle(MessageViewHolder holder, ChatMessage message,
                                 int foreground, int accent) {
            boolean hasArticle = message.getArticleId() != null;
            holder.articleBlock.setVisibility(hasArticle ? View.VISIBLE : View.GONE);
            if (!hasArticle) {
                holder.articleBlock.setOnClickListener(null);
                return;
            }
            holder.articleTitle.setText(message.getArticleTitle());
            holder.articleTitle.setTextColor(foreground);
            holder.articleCta.setTextColor(accent);
            holder.articleTile.setBackgroundTintList(ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(foreground, 0x26)));
            holder.articleBlock.setOnClickListener(v -> startActivity(
                    ArticleView.intent(GroupChatActivity.this, message.getArticleId())));
        }

        private void bindPhoto(MessageViewHolder holder, ChatMessage message) {
            boolean hasPhoto = ChatMessage.MEDIA_IMAGE.equals(message.getMediaType())
                    && message.getMediaPath() != null;
            holder.image.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
            if (!hasPhoto) {
                holder.image.setOnClickListener(null);
                return;
            }
            ImageLoader.load(holder.image,
                    MediaFiles.chatFile(GroupChatActivity.this, message.getMediaPath()),
                    Ui.dp(GroupChatActivity.this, 200));
            holder.image.setOnClickListener(v -> startActivity(ImageViewerActivity.intent(
                    GroupChatActivity.this, message.getMediaPath())));
        }

        private void bindAudio(MessageViewHolder holder, ChatMessage message,
                               int foreground, int background) {
            boolean hasAudio = ChatMessage.MEDIA_AUDIO.equals(message.getMediaType())
                    && message.getMediaPath() != null;
            holder.audioBlock.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
            if (!hasAudio) {
                holder.play.setOnClickListener(null);
                return;
            }
            // Round play button in the text colour, with the icon in the bubble colour
            holder.play.setBackgroundTintList(ColorStateList.valueOf(foreground));
            holder.play.setColorFilter(background);
            holder.audioProgress.setIndicatorColor(foreground);
            holder.audioProgress.setTrackColor(ColorUtils.setAlphaComponent(foreground, 0x40));
            holder.audioDuration.setTextColor(foreground);
            holder.play.setOnClickListener(v -> toggleAudio(message));
            bindAudioState(holder, message);
        }

        /** Play or pause icon, progress bar and time for the current playback position. */
        private void bindAudioState(MessageViewHolder holder, ChatMessage message) {
            boolean active = message.getId() == playingMessageId && player != null;
            boolean playing = active && player.isPlaying();
            holder.play.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
            holder.play.setContentDescription(getString(playing
                    ? R.string.chat_voice_pause : R.string.chat_voice_play));
            long total = Math.max(1, message.getMediaDurationMs());
            long position = active ? player.getCurrentPosition() : 0;
            holder.audioProgress.setProgressCompat((int) Math.min(1000, position * 1000 / total), false);
            holder.audioDuration.setText(MediaFiles.formatDuration(active ? position : total));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final LinearLayout row, articleBlock, audioBlock;
            final MaterialCardView bubble;
            final ShapeableImageView image;
            final ImageButton play;
            final LinearProgressIndicator audioProgress;
            final TextView avatar, sender, body, time, articleTile, articleTitle, articleCta,
                    audioDuration;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                row = itemView.findViewById(R.id.messageRow);
                avatar = itemView.findViewById(R.id.messageAvatar);
                bubble = itemView.findViewById(R.id.messageBubble);
                sender = itemView.findViewById(R.id.tvSender);
                body = itemView.findViewById(R.id.tvMessage);
                time = itemView.findViewById(R.id.tvMessageTime);
                articleBlock = itemView.findViewById(R.id.articleBlock);
                articleTile = itemView.findViewById(R.id.tvArticleTile);
                articleTitle = itemView.findViewById(R.id.tvSharedArticleTitle);
                articleCta = itemView.findViewById(R.id.tvSharedArticleCta);
                image = itemView.findViewById(R.id.ivMessageImage);
                audioBlock = itemView.findViewById(R.id.audioBlock);
                play = itemView.findViewById(R.id.btnPlayAudio);
                audioProgress = itemView.findViewById(R.id.progressAudio);
                audioDuration = itemView.findViewById(R.id.tvAudioDuration);
            }
        }
    }
}
