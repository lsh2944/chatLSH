package com.example.myapplication.messages

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.models.ChatMessage
import com.example.myapplication.models.User
import com.example.myapplication.registerlogin.RegisterActivity
import com.example.myapplication.views.ChatFromItem
import com.example.myapplication.views.ChatToItem
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import java.util.*

var checker = ""

var myUrl = ""

private var loadingBar: ProgressDialog? = null

private var fileUri: Uri? = null

private var messageReceiverID: String? = null

private var messageSenderID: String? = null

private var uploadTask: StorageTask<*>? = null

private var RootRef: DatabaseReference? = null

private var saveCurrentTime: String? = null
private var saveCurrentDate: String? = null

private var MessageInputText: EditText? = null

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ChatLogActivity : AppCompatActivity() {


  companion object {
    val TAG = "ChatLog"
  }

  val adapter = GroupAdapter<ViewHolder>()

  var toUser: User? = null

  var SendFilesButton: Button? = null






  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_chat_log)

    recyclerview_chat_log.adapter = adapter

    toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)

    supportActionBar?.title = toUser?.username

//    setupDummyData()
    listenForMessages()

    send_button_chat_log.setOnClickListener {
      Log.d(TAG, "Attempt to send message....")
      performSendMessage()
    }

    //*************이미지 송신버튼 만드는중임**************//


    RootRef = FirebaseDatabase.getInstance().reference
    SendFilesButton = findViewById<View>(R.id.send_file_button_chat_log) as Button



    send_file_button_chat_log!!.setOnClickListener {
      val options = arrayOf<CharSequence>(
        "Images",
        "PDF Files",
        "Ms Word Files"
      )

      val builder = AlertDialog.Builder(this@ChatLogActivity)
      builder.setTitle("Select the File")
      builder.setItems(options) { dialogInterface, i ->
        if (i == 0) {
          checker = "image"
          val intent = Intent()
          intent.action = Intent.ACTION_GET_CONTENT
          intent.type = "image/*"
          startActivityForResult(Intent.createChooser(intent, "Select Image"), 438)
        }
        if (i == 1) {
          checker = "pdf"
          val intent = Intent()
          intent.action = Intent.ACTION_GET_CONTENT
          intent.type = "application/pdf*"
          startActivityForResult(Intent.createChooser(intent, "Select pdf file"), 438)
        }
        if (i == 2) {
          checker = "docx"
          val intent = Intent()
          intent.action = Intent.ACTION_GET_CONTENT
          intent.type = "application/msword"
          startActivityForResult(Intent.createChooser(intent, "Select Ms Word File"), 438)
        }
      }
      builder.show()
      uploadImageToFirebaseStorage()
    }
  }
  var selectedPhotoUri: Uri? = null

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
      // proceed and check what the selected image was....

      if (data != null) {
        selectedPhotoUri = data.data

        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

//      selectphoto_imageview_register.setImageBitmap(bitmap)  //***//00
//
//      selectphoto_button_register.alpha = 0f     //***//
      }
    }
  }


  private fun uploadImageToFirebaseStorage() {
    if (selectedPhotoUri == null) return

    val filename = UUID.randomUUID().toString()
    val ref = FirebaseStorage.getInstance().getReference("/message images/$filename")

    ref.putFile(selectedPhotoUri!!)
      .addOnSuccessListener {
        Log.d(RegisterActivity.TAG, "Successfully uploaded image: ${it.metadata?.path}")

        ref.downloadUrl.addOnSuccessListener {
          Log.d(RegisterActivity.TAG, "File Location: $it")

          savePhotoToFirebaseDatabase(it.toString())
        }
      }
      .addOnFailureListener {
        Log.d(RegisterActivity.TAG, "Failed to upload image to storage: ${it.message}")
      }
  }

  private fun savePhotoToFirebaseDatabase(MessageImageUrl: String) {
    val uid = FirebaseAuth.getInstance().uid ?: ""
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    val user = User(uid, username_edittext_register.text.toString(), MessageImageUrl)

    ref.setValue(user)
      .addOnSuccessListener {
        Log.d(RegisterActivity.TAG, "Finally we saved the user to Firebase Database")

        val intent = Intent(this, LatestMessagesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

      }
      .addOnFailureListener {
        Log.d(RegisterActivity.TAG, "Failed to set value to database: ${it.message}")
      }
  }


//  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//    super.onActivityResult(requestCode, resultCode, data)
//
//    loadingBar = ProgressDialog(this)
//
//    if (requestCode == 438 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
//      loadingBar!!.setTitle("Sending File")
//      loadingBar!!.setMessage("Please wait, We are sending this file...")
//      loadingBar!!.setCanceledOnTouchOutside(false)
//      loadingBar!!.show();
//      fileUri = data.data
//      if (checker != "image") {
//      } else if (checker == "image") {
//        val storageReference = FirebaseStorage.getInstance().reference.child("Image Files")
//        //val messageSenderRef = "Messages/$messageSenderID/$messageReceiverID"
//        //val messageReceiverRef = "Messages/$messageReceiverID/$messageSenderID"
//        val userMessageKeyRef = RootRef!!.child("Messages")
//          .child(messageSenderID!!).child(messageReceiverID!!).push()
//        val messagePushID = userMessageKeyRef.key
//        val filePath = storageReference.child("$messagePushID.jpg")
//        uploadTask = filePath.putFile(fileUri!!)
//
//
//        (uploadTask as UploadTask).continueWithTask { task ->
//          if (!task.isSuccessful) {
//          }
//          filePath.downloadUrl
//        }.addOnCompleteListener(OnCompleteListener<Uri?> { task ->
//          if (task.isSuccessful) {
//            val downloadUrl = task.result
//            myUrl = downloadUrl.toString()
//            val messageTextBody: MutableMap<String, String?> = HashMap()
//            messageTextBody["message"] = myUrl
//            messageTextBody["name"] = fileUri!!.lastPathSegment
//            messageTextBody["type"] = checker
//            messageTextBody["to"] = messageReceiverID
//            messageTextBody["messageID"] = messagePushID
//            messageTextBody["time"] = saveCurrentTime
//            messageTextBody["date"] = saveCurrentDate
//            val messageBodyDetails: MutableMap<String, Any> = HashMap()
//           // messageBodyDetails["$messageSenderRef/$messagePushID"] = messageTextBody
//           // messageBodyDetails["$messageReceiverRef/$messagePushID"] = messageTextBody
////
////
//            RootRef!!.updateChildren(messageBodyDetails).addOnCompleteListener{task ->
//              if(task.isSuccessful){
//                loadingBar!!.dismiss()
//                Toast.makeText(this@ChatLogActivity, "Message Sent Successfully...", Toast.LENGTH_SHORT).show()
//              }else{
//                loadingBar!!.dismiss()
//                Toast.makeText(this@ChatLogActivity, "Error", Toast.LENGTH_SHORT).show()
//              }
//              MessageInputText!!.setText("")
//
//            }
//          }
//        })
//      } else {
//        loadingBar!!.dismiss()
//        Toast.makeText(this, "Nothing Selected, Error.", Toast.LENGTH_SHORT).show()
//      }
//    }
//  }

  //*************이미지 송신버튼 만드는중임**************//


  private fun listenForMessages() {
    val fromId = FirebaseAuth.getInstance().uid
    val toId = toUser?.uid
    val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

    ref.addChildEventListener(object: ChildEventListener {

      override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val chatMessage = p0.getValue(ChatMessage::class.java)

        if (chatMessage != null) {
          Log.d(TAG, chatMessage.text)

          if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
            val currentUser = LatestMessagesActivity.currentUser ?: return
            adapter.add(ChatFromItem(chatMessage.text, currentUser))
          } else {
            adapter.add(ChatToItem(chatMessage.text, toUser!!))
          }
        }

        recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)

      }

      override fun onCancelled(p0: DatabaseError) {

      }

      override fun onChildChanged(p0: DataSnapshot, p1: String?) {

      }

      override fun onChildMoved(p0: DataSnapshot, p1: String?) {

      }

      override fun onChildRemoved(p0: DataSnapshot) {

      }

    })

  }

  private fun performSendMessage() {
    // how do we actually send a message to firebase...
    val text = edittext_chat_log.text.toString()

    val fromId = FirebaseAuth.getInstance().uid
    val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
    val toId = user.uid

    if (fromId == null) return

//    val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
    val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

    val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

    val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000)

    reference.setValue(chatMessage)
        .addOnSuccessListener {
          Log.d(TAG, "Saved our chat message: ${reference.key}")
          edittext_chat_log.text.clear()
          recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
        }

    toReference.setValue(chatMessage)

    val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
    latestMessageRef.setValue(chatMessage)

    val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
    latestMessageToRef.setValue(chatMessage)
  }

}
