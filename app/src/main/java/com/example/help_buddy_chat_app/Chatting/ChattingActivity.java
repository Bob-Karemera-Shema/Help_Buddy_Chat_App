package com.example.help_buddy_chat_app.Chatting;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.Common.Extras;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.R;
import com.example.help_buddy_chat_app.SelectFriend.SelectFriendActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChattingActivity extends AppCompatActivity implements View.OnClickListener {

    //Declare UI views
    private ImageView ivSend, ivAttachment, ivProfile;
    private TextView tvUserName, tvUserStatus;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private SwipeRefreshLayout srlMessages;

    private MessageAdapter messageAdapter;
    private List<MessageModel> messageModelList;

    //On each screen a page of messages is displayed and when the user scrolls up for old messages
    //the currentPage number is incremented to load from messages
    private int currentPage =1;
    private static final  int messages_per_page = 30;

    //Declare Database reference to root
    private DatabaseReference mRootRef;
    private FirebaseAuth firebaseAuth;
    //Reference to messages node on database
    private DatabaseReference databaseReferenceMessages;

    //listener for new message records on database
    private ChildEventListener childEventListener;

    private String currentUserId, chatUserId;

    private BottomSheetDialog bottomSheetDialog;

    private static final int requestCodeToPickImage = 101;
    private static final int requestCodeToCaptureImage = 102;
    private static final int requestCodeToPickVideo = 103;
    private static final int requestCodeToForwadMessage = 104;

    //file upload layout declaration
    private LinearLayout llProgress;

    private String userName, userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setTitle("");
            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.chat_action_bar, null);

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setElevation(0);
            actionBar.setCustomView(actionBarLayout);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions()|ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        //initialise UI views and database references
        ivSend = findViewById(R.id.ivSend);
        ivAttachment = findViewById(R.id.ivAttachment);
        etMessage = findViewById(R.id.etMessage);
        llProgress = findViewById(R.id.llProgress);
        ivProfile = findViewById(R.id.ivChatProfile);
        tvUserName = findViewById(R.id.tvChatUserName);
        tvUserStatus = findViewById(R.id.tvUserStatus);

        ivSend.setOnClickListener(this);
        ivAttachment.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        //fetch friend information from chatlist activity
        if(getIntent().hasExtra(Extras.user_key))
        {
            chatUserId = getIntent().getStringExtra(Extras.user_key);
        }
        if(getIntent().hasExtra(Extras.user_name))
        {
            userName = getIntent().getStringExtra(Extras.user_name);
        }
        if(getIntent().hasExtra(Extras.user_picture))
        {
            userProfile = getIntent().getStringExtra(Extras.user_picture);
        }

        //Set the information fetched
        tvUserName.setText(userName);

        if(!TextUtils.isEmpty(userProfile))
        {
            StorageReference fileLocation = FirebaseStorage.getInstance().getReference().child(Constants.imageLocation).child(userProfile);
            fileLocation.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(ChattingActivity.this)
                            .load(uri)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(ivProfile);
                }
            });
        }

        rvMessages = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);

        messageModelList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageModelList);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        //getMessages
        getMessages();

        //set the number of unread messages to zero as unread messages have been opened
        mRootRef.child(NodeNames.chats).child(currentUserId).child(chatUserId)
                .child(NodeNames.unread_count).setValue(0);

        //ensure screen displays the latest record
        rvMessages.scrollToPosition(messageModelList.size()-1);

        //set listener for swipe event when the user wants to load older messages
        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //increment message page and load more messages
                currentPage++;
                getMessages();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);

        //set on click listeners for file sharing options
        view.findViewById(R.id.llCamera).setOnClickListener(this);
        view.findViewById(R.id.llGallery).setOnClickListener(this);
        view.findViewById(R.id.llVideo).setOnClickListener(this);
        view.findViewById(R.id.ivClose).setOnClickListener(this);

        bottomSheetDialog.setContentView(view);

        if(getIntent().hasExtra(Extras.message) && getIntent().hasExtra(Extras.message)
                && getIntent().hasExtra(Extras.message))
        {
            //if opening the chatting activity from a forwarding message action,
            //add a new message record for the forwarded message on the database
            String message = getIntent().getStringExtra(Extras.message);
            String messageID = getIntent().getStringExtra(Extras.message_id);
            String messageType = getIntent().getStringExtra(Extras.message_type);

            DatabaseReference messageReference = mRootRef.child(NodeNames.messages)
                    .child(currentUserId).child(chatUserId).push();

            String newMessageId = messageReference.getKey();
            if(messageType.equals(Constants.messageTypeText))
            {
                //if message is of type text, just make a new message record on the database
                sendMessage(message, messageType, newMessageId);
            }
            else
            {
                //if message is of type video/image, create a new copy of the file on the database
                //storage location and make a new message record
                StorageReference storageRoot = FirebaseStorage.getInstance().getReference();
                String folder = messageType.equals(Constants.messageTypeVideo)?
                        Constants.video_messages : Constants.image_messages;
                String oldFile = messageType.equals(Constants.messageTypeVideo)?
                        messageID + ".mp4" : messageID + ".jpg";
                String newFile = messageType.equals(Constants.messageTypeVideo)?
                        newMessageId + ".mp4" : newMessageId + ".jpg";

                String localFileLocation = getExternalFilesDir(null).getAbsolutePath() + "/" + oldFile;
                File localFile = new File(localFileLocation);

                StorageReference newFileLocation = storageRoot.child(folder).child(newFile);
                storageRoot.child(folder).child(oldFile).getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        UploadTask uploadTask = newFileLocation.putFile(Uri.fromFile(localFile));
                        uploadProgress(uploadTask, newFileLocation, newMessageId, messageType);
                    }
                });
            }
        }

        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.users).child(chatUserId);
        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = "";
                if(snapshot.child(NodeNames.online).getValue() != null)
                    //get user's online status
                    status = snapshot.child(NodeNames.online).getValue().toString();

                if(status.equals("true"))
                    tvUserStatus.setText(Constants.status_online);
                else
                    tvUserStatus.setText(Constants.status_offline);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                DatabaseReference currentUserRef = mRootRef.child(NodeNames.chats)
                        .child(currentUserId).child(chatUserId);

                if(editable.toString().matches(""))
                {
                    currentUserRef.child(NodeNames.typing).setValue(Constants.typing_stopped);
                }
                else
                {
                    currentUserRef.child(NodeNames.typing).setValue(Constants.typing_started);
                }
            }
        });


        DatabaseReference chatUserRef = mRootRef.child(NodeNames.chats).child(chatUserId).child(currentUserId);
        chatUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(NodeNames.typing).getValue() != null)
                {
                    String typingStatus = snapshot.child(NodeNames.typing).getValue().toString();
                    if(typingStatus.equals(Constants.typing_started))
                        tvUserStatus.setText(Constants.status_typing);
                    else
                        tvUserStatus.setText(Constants.status_online);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.ivSend:
                if(Connection.connected(this)) {
                    DatabaseReference userMessagePush = mRootRef.child(NodeNames.messages)
                            .child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.messageTypeText, pushId);
                }
                else
                {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.ivAttachment:
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
                {
                    if(bottomSheetDialog!=null)
                        bottomSheetDialog.show();
                }
                else
                {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                }

                //hide keyboard when attachment icon is clicked
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if(inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(),0);

                break;

            case R.id.llCamera:
                bottomSheetDialog.dismiss();
                Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCamera, requestCodeToCaptureImage);
                break;

            case R.id.llGallery:
                bottomSheetDialog.dismiss();
                Intent intentImage = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentImage, requestCodeToPickImage);
                break;

            case R.id.llVideo:
                bottomSheetDialog.dismiss();
                Intent intentVideo = new Intent(Intent.ACTION_PICK,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentVideo, requestCodeToPickVideo);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK)
        {
            if(requestCode == requestCodeToCaptureImage)
            {
                //Camera
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                uploadBytes(bytes, Constants.messageTypeImage);
            }

            if(requestCode == requestCodeToPickImage)
            {
                //Gallery
                Uri uri = data.getData();
                uploadFile(uri, Constants.messageTypeImage);
            }

            if(requestCode == requestCodeToPickVideo)
            {
                //Video
                Uri uri = data.getData();
                uploadFile(uri, Constants.messageTypeVideo);
            }

            if(requestCode == requestCodeToForwadMessage)
            {
                //request code to forward message
                Intent intent = new Intent(this, ChattingActivity.class);

                intent.putExtra(Extras.user_key, data.getStringExtra(Extras.user_key));
                intent.putExtra(Extras.user_name, data.getStringExtra(Extras.user_name));
                intent.putExtra(Extras.user_picture, data.getStringExtra(Extras.user_picture));

                intent.putExtra(Extras.message, data.getStringExtra(Extras.message));
                intent.putExtra(Extras.message_id, data.getStringExtra(Extras.message_id));
                intent.putExtra(Extras.message_type, data.getStringExtra(Extras.message_type));

                startActivity(intent);
                finish();
            }
        }
    }

    private void uploadFile( Uri uri, String messageType)
    {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.messages).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        //Store Message record
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.messageTypeVideo)? Constants.video_messages : Constants.image_messages;
        String fileName = messageType.equals(Constants.messageTypeVideo)? pushId + ".mp4" : pushId + ".jpg";

        //Upload file to Firebase Storage database
        StorageReference fileLocation = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileLocation.putFile(uri);

        uploadProgress(uploadTask, fileLocation, pushId, messageType);
    }

    private void uploadBytes(ByteArrayOutputStream bytes, String messageType)
    {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.messages).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        //Store Message record
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.messageTypeVideo)? Constants.video_messages : Constants.image_messages;
        String fileName = messageType.equals(Constants.messageTypeVideo)? pushId + ".mp4" : pushId + ".jpg";

        //Upload file to Firebase Storage database
        StorageReference fileLocation = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileLocation.putBytes(bytes.toByteArray());

        uploadProgress(uploadTask, fileLocation, pushId, messageType);
    }

    private void uploadProgress(final UploadTask task, StorageReference fileLocation, String pushId, String messageType)
    {
        View view = getLayoutInflater().inflate(R.layout.file_progress, null);
        ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
        TextView tvProgress = view.findViewById(R.id.tvFileProgress);
        ImageView ivPlay = view.findViewById(R.id.ivPlay);
        ImageView ivPause = view.findViewById(R.id.ivPause);
        ImageView ivCancel = view.findViewById(R.id.ivCancel);

        //Add on click listeners for file upload buttons
        ivPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.pause();
                ivPlay.setVisibility(View.VISIBLE);
                ivPause.setVisibility(View.GONE);
            }
        });

        ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.resume();
                ivPlay.setVisibility(View.GONE);
                ivPause.setVisibility(View.VISIBLE);
            }
        });

        ivCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.cancel();
            }
        });

        llProgress.addView(view);
        tvProgress.setText(getString(R.string.upload_progress, messageType, "0"));

        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = (100.0 * snapshot.getBytesTransferred()/snapshot.getTotalByteCount());

                pbProgress.setProgress((int) progress);
                tvProgress.setText(getString(R.string.upload_progress, messageType, String.valueOf(pbProgress.getProgress())));
            }
        });

        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                llProgress.removeView(view);
                if(task.isSuccessful())
                {
                    fileLocation.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            sendMessage(downloadUrl, messageType, pushId);
                        }
                    });
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                llProgress.removeView(view);
                Toast.makeText(ChattingActivity.this,
                        getString(R.string.failed_to_send_file, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if(bottomSheetDialog!=null)
                    bottomSheetDialog.show();
            }
            else
            {
                Toast.makeText(this, "Permission Required to access files", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String message, String messageType, String pushId)
    {
        try {
            if(!message.equals(""))
            {
                //Declare HashMap to store message to database
                HashMap messageMap = new HashMap();
                messageMap.put(NodeNames.messageId, pushId);
                messageMap.put(NodeNames.message, message);
                messageMap.put(NodeNames.messageType, messageType);
                messageMap.put(NodeNames.messageFrom, currentUserId);
                messageMap.put(NodeNames.messageTime, ServerValue.TIMESTAMP);

                String currentUserRef = NodeNames.messages + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.messages + "/" + chatUserId + "/" + currentUserId;

                HashMap messageUserMap = new HashMap();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                etMessage.setText("");

                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if(error != null)
                        {
                            Toast.makeText(ChattingActivity.this,
                                    getString(R.string.failed_to_message, error.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(ChattingActivity.this, R.string.message_sent,
                                    Toast.LENGTH_SHORT).show();

                            //Send message notification
                            String title = "";

                            if(messageType.equals(Constants.messageTypeText))
                                title = "New Message";

                            if(messageType.equals(Constants.messageTypeImage))
                                title = "New Image Message";

                            if(messageType.equals(Constants.messageTypeVideo))
                                title = "New Video Message";

                            Connection.sendNotification(ChattingActivity.this, title, message, chatUserId);
                            //update chat features on receiver end

                            String lastMessage = !title.equals("New Message")? title:message;
                            Connection.updateChatFeatures(ChattingActivity.this, currentUserId, chatUserId, lastMessage);
                        }
                    }
                });
            }
        }
        catch(Exception exception)
        {
            Toast.makeText(ChattingActivity.this,
                    getString(R.string.failed_to_message, exception.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void getMessages()
    {
        //first clear message list to load a new list
        messageModelList.clear();
        //get reference to database messages node
        databaseReferenceMessages = mRootRef.child(NodeNames.messages)
                .child(currentUserId).child(chatUserId);

        /*query the database for message records and only limit them to the 30 records
          if the user swipes up 30 more records are retrieved
          that means everytime the current page will be incremented, it will be multiplied by the
          number of messages per page resulting in  the desired number of messages*/
        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * messages_per_page);

        if(childEventListener != null)
            messageQuery.removeEventListener(childEventListener);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                /*For each message object received, all object attributes are initialised.
                Unlike in previous attempts in the home page fragments where a single value from
                the database is fetched and assigned to a string variable.*/

                MessageModel message = snapshot.getValue(MessageModel.class);

                messageModelList.add(message);
                //notify message adapter about new message record from database
                messageAdapter.notifyDataSetChanged();


                //ensure screen immediately displays the latest message as soon as it is received
                rvMessages.scrollToPosition(messageModelList.size()-1);
                //set swipe refresh to false until message list is completely loaded
                srlMessages.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                getMessages();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                srlMessages.setRefreshing(false);
            }
        };
        messageQuery.addChildEventListener(childEventListener);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId)
        {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(String messageID, String messageType)
    {
        //method to delete a message
        DatabaseReference databaseReferenceCurrentUser = mRootRef.child(NodeNames.messages)
                .child(currentUserId).child(chatUserId).child(messageID);

        databaseReferenceCurrentUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    DatabaseReference databaseReferenceChatUser = mRootRef.child(NodeNames.messages)
                            .child(chatUserId).child(currentUserId).child(messageID);
                    databaseReferenceChatUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                Toast.makeText(ChattingActivity.this, R.string.message_deleted, Toast.LENGTH_SHORT).show();

                                if(!messageType.equals(Constants.messageTypeText))
                                {
                                    //if the message is an image/video file, delete the file as well
                                    StorageReference rootReference = FirebaseStorage.getInstance().getReference();
                                    String folder = messageType.equals(Constants.messageTypeVideo)
                                            ? Constants.video_messages : Constants.image_messages;
                                    String fileName = messageType.equals(Constants.messageTypeVideo)
                                            ? messageID + ".mp4" : messageID + ".jpg";
                                    StorageReference fileLocation = rootReference.child(folder).child(fileName);

                                    fileLocation.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(!task.isSuccessful())
                                            {
                                                Toast.makeText(ChattingActivity.this,
                                                        getString(R.string.failed_to_delete_file, task.getException()),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            }
                            else
                            {
                                Toast.makeText(ChattingActivity.this,
                                        getString(R.string.failed_to_delete, task.getException()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else
                {
                    Toast.makeText(ChattingActivity.this,
                            getString(R.string.failed_to_delete, task.getException()),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void downloadFile(String messageId, String messageType, boolean isShare)
    {
        //method to download an image or video
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else
        {
            String folder = messageType.equals(Constants.messageTypeVideo)
                    ? Constants.video_messages : Constants.image_messages;
            String fileName = messageType.equals(Constants.messageTypeVideo)
                    ? messageId + ".mp4" : messageId + ".jpg";

            StorageReference fileLocation = FirebaseStorage.getInstance().getReference()
                .child(folder).child(fileName);

            //Access location where images/videos are stored on the device storage
            String localFileLocation = getExternalFilesDir(null).getAbsolutePath() + "/" + fileName;
            File localFile = new File(localFileLocation); //create local file in specified location

            try
            {
                if(localFile.exists() || localFile.createNewFile())
                {
                    FileDownloadTask downloadTask = fileLocation.getFile(localFile);

                    View view = getLayoutInflater().inflate(R.layout.file_progress, null);
                    ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
                    TextView tvProgress = view.findViewById(R.id.tvFileProgress);
                    ImageView ivPlay = view.findViewById(R.id.ivPlay);
                    ImageView ivPause = view.findViewById(R.id.ivPause);
                    ImageView ivCancel = view.findViewById(R.id.ivCancel);

                    //Add on click listeners for file upload buttons
                    ivPause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.pause();
                            ivPlay.setVisibility(View.VISIBLE);
                            ivPause.setVisibility(View.GONE);
                        }
                    });

                    ivPlay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.resume();
                            ivPlay.setVisibility(View.GONE);
                            ivPause.setVisibility(View.VISIBLE);
                        }
                    });

                    ivCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.cancel();
                        }
                    });

                    llProgress.addView(view);
                    tvProgress.setText(getString(R.string.download_progress, messageType, "0"));

                    downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                            double progress = (100.0 * snapshot.getBytesTransferred()/snapshot.getTotalByteCount());

                            pbProgress.setProgress((int) progress);
                            tvProgress.setText(getString(R.string.download_progress, messageType, String.valueOf(pbProgress.getProgress())));
                        }
                    });

                    downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            llProgress.removeView(view);
                            if(task.isSuccessful())
                            {

                                if(isShare)
                                {
                                    Intent intentShare = new Intent();
                                    intentShare.setAction(Intent.ACTION_SEND);
                                    intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(localFileLocation));

                                    if(messageType.equals(Constants.messageTypeVideo))
                                        intentShare.setType("video/mp4");
                                    if(messageType.equals(Constants.messageTypeImage))
                                        intentShare.setType("image/jpg");

                                    startActivity(Intent.createChooser(intentShare, getString(R.string.share_with)));
                                }
                                else
                                {
                                    Snackbar snackbar = Snackbar.make(llProgress, getString(R.string.file_downloaded)
                                            , Snackbar.LENGTH_INDEFINITE);

                                    snackbar.setAction(R.string.view, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Uri uri = Uri.parse(localFileLocation);
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                                            if (messageType.equals(Constants.messageTypeVideo))
                                                intent.setDataAndType(uri, "video/mp4");
                                            if (messageType.equals(Constants.messageTypeImage))
                                                intent.setDataAndType(uri, "image/jpg");

                                            startActivity(intent);
                                        }
                                    });

                                    snackbar.show();
                                }
                            }
                        }
                    });

                    downloadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            llProgress.removeView(view);
                            Toast.makeText(ChattingActivity.this,
                                    getString(R.string.failed_to_download, e.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    Toast.makeText(this, R.string.failed_to_download_file, Toast.LENGTH_SHORT).show();
                }
            }
            catch (Exception e)
            {
                Toast.makeText(ChattingActivity.this,
                        getString(R.string.failed_to_download, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void forwardMessage(String selectedMessageID, String selectedMessage, String selectedMessageType) {
        Intent intent = new Intent(this, SelectFriendActivity.class);
        intent.putExtra(Extras.message, selectedMessage);
        intent.putExtra(Extras.message_id, selectedMessageID);
        intent.putExtra(Extras.message_type, selectedMessageType);

        startActivityForResult(intent, requestCodeToForwadMessage);
    }

    @Override
    public void onBackPressed() {
        //set the number of unread messages to zero as unread messages have been opened
        mRootRef.child(NodeNames.chats).child(currentUserId).child(chatUserId)
                .child(NodeNames.unread_count).setValue(0);
        super.onBackPressed();
    }
}