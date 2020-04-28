package com.example.cryptochat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static String myName = "";

    RecyclerView chatWindow;
    private MessageController controller;
    private Server server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your name");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                myName = input.getText().toString();
                TextView usersOnline = findViewById(R.id.myName);
                usersOnline.setText(myName);
                server.sendName(myName);
            }
        });
        builder.show();

        chatWindow = findViewById(R.id.chatWindow);
        controller = new MessageController();
        controller
                .setIncomingLayout(R.layout.incoming_message)
                .setOutgoingLayout(R.layout.outgoing_message)
                .setMessageTextId(R.id.messageText)
                .setMessageTimeId(R.id.messageTime)
                .setUserNameId(R.id.userName)
                .appendTo(chatWindow, this);

        final EditText chatInput = findViewById(R.id.chatInput);
        Button sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = chatInput.getText().toString();
                controller.addMessage(
                        new MessageController.Message(text, myName, true)
                );
                chatInput.setText("");
                server.sendMessage(text);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        server = new Server(new Consumer<Pair<String, String>>() {
            @Override
            public void accept(final Pair<String, String> pair) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        controller.addMessage(
                                new MessageController.Message(pair.second, pair.first, false)
                        );
                    }
                });
            }
        }, new Consumer<String>() {
            @Override
            public void accept(final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (s != null && s.length() > 0) {
                            Toast toast = Toast.makeText(MainActivity.this,
                                    s + " подключился к чату", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.TOP, 0, 100);
                            toast.show();
                        }
                    }
                });
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(final Integer i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView usersOnline = findViewById(R.id.usersOnline);
                        String text = "Пользователей онлайн: " + i;
                        usersOnline.setText(text);
                    }
                });
            }
        });
        server.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        server.disconnect();
    }
}
