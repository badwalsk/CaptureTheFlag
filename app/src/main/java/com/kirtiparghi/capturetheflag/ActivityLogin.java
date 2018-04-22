package com.kirtiparghi.capturetheflag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.Serializable;

public class ActivityLogin extends Activity
{
    //defaults
    public SharedPreferences.Editor loginPrefsEditor;
    public  SharedPreferences loginPreferences;
    private Boolean saveLogin;

    //get Text
    EditText edtEmail, edtPassword;
    Button btnLogin;
    private RadioGroup radioUserTypeGroup;
    private RadioGroup radioPlayerTeamGroup;
    RadioButton radioUserType, radioPlayerTeam;

    //database
    FirebaseDatabase db;
    DatabaseReference root;

    String strPlayerTeam, strUserType;
    LinearLayout linearlayout;
    TextView labelUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_login);

        if (getStatusList(this)!= null) {
            Toast.makeText(ActivityLogin.this, "get"+getStatusList(this).player,Toast.LENGTH_SHORT).show();
            Player current=getStatusList(this);
            Intent intent = new Intent(getApplicationContext(), ActivityPlayerHome.class);
            intent.putExtra("player",current);
            startActivity(intent);
        }

        strPlayerTeam = "";
        strUserType = "";
        labelUser = (TextView)findViewById(R.id.labelTeam);

        /* Fetching all data from field*/

        edtEmail = (EditText) findViewById(R.id.user_email);

        edtPassword = (EditText) findViewById(R.id.password);

        btnLogin = (Button) findViewById(R.id.login_button);
        radioPlayerTeamGroup = (RadioGroup) findViewById(R.id.radioTeams);
        radioUserTypeGroup =  (RadioGroup) findViewById(R.id.radioPlayers);
        linearlayout = (LinearLayout) findViewById(R.id.playerType);

        // setup the firebase variables
        db = FirebaseDatabase.getInstance();
        root = db.getReference();

        btnLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final String username = edtEmail.getText().toString().trim();
                final String password = edtPassword.getText().toString().trim();

                validateFields();
                Log.d("err"  , username);
                Log.d("err"  , password);
                if (username.isEmpty()) {
                    // if message is blank, then quit
                    Toast.makeText(ActivityLogin.this, "Please Enter an Email",Toast.LENGTH_SHORT).show();
                    return;
                }



                if (username.equals("admin@gmail.com")) {
                    Log.e("ctf","inside if....");
                    SharedPreferences sharedpreferences = getSharedPreferences("ctf", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("email","admin@gmail.com");
                    editor.putString("isPlayer","false");
                    editor.commit();
                    Intent intent = new Intent(getApplicationContext(), ActivityAdminHome.class);
                    startActivity(intent);
                }
                else {
                    if (password.isEmpty()) {
                        // if message is blank, then quit
                        Toast.makeText(ActivityLogin.this, "Please Enter an Password",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    performLoginOrAccountCreation(username,password,strPlayerTeam);
                }

            }
        });


        radioUserTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId=radioUserTypeGroup.getCheckedRadioButtonId();
                radioUserType=(RadioButton)findViewById(selectedId);
                Toast.makeText(ActivityLogin.this,radioUserType.getText(),Toast.LENGTH_SHORT).show();
                if (radioUserType.getText().toString().equals("ADMIN"))
                {
                    strUserType = "ADMIN";
                    linearlayout.setVisibility(View.GONE);
                    labelUser.setVisibility(View.GONE);
                    edtPassword.setVisibility(View.GONE);

                }
                else {
                    strUserType = "PLAYER";
                    linearlayout.setVisibility(View.VISIBLE);
                }
            }
        });

        radioPlayerTeamGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId=radioPlayerTeamGroup.getCheckedRadioButtonId();
                radioPlayerTeam=(RadioButton)findViewById(selectedId);
                Toast.makeText(ActivityLogin.this,radioPlayerTeam.getText(),Toast.LENGTH_SHORT).show();
                if (radioPlayerTeam.getText().toString().equals("Team A")) {
                    strPlayerTeam = "Team A";
                }
                else {
                    strPlayerTeam = "Team B";
                }
            }
        });
    }

    private void validateFields() {
            String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
            if (!edtEmail.getText().toString().matches(emailPattern))
            {
                Toast.makeText(getApplicationContext(),"Please Enter a valid email address",Toast.LENGTH_SHORT).show();
            }
    }

    private void performLoginOrAccountCreation(final String email, final String password,final String strPlayerTeam)
    {
        Query query = root.child("Player").orderByChild("player").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // dataSnapshot is the "issue" node with all children with id 0

                    for (DataSnapshot player : dataSnapshot.getChildren()) {
                        // do something with the individual "issues"
                        Player currentPlayer = player.getValue(Player.class);

                        if (currentPlayer.passcode.equals(password)) {

                            //save default
                            ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getApplicationContext(), "object_prefs", 0);
                            complexPreferences.putObject("object_value", currentPlayer);
                            complexPreferences.commit();

                            Toast.makeText(getApplicationContext(), "User Found"+currentPlayer.getPlayer(), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getApplicationContext(), ActivityPlayerHome.class);
                            intent.putExtra("player",currentPlayer);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Password is wrong", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "User not found, Creating new User", Toast.LENGTH_LONG).show();
                    String id = root.child("Player").push().getKey();
                    Player newPlayer = new Player(id,email,password,strPlayerTeam,"","");
                    root.child("Player").child(id).setValue(newPlayer);
                    Toast.makeText(getApplicationContext(), "New Player Added "+email, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getApplicationContext(), ActivityPlayerHome.class);
                    intent.putExtra("player",newPlayer);
                    startActivity(intent);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static Player getStatusList(Context ctx){
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "object_prefs", 0);
        Player current = complexPreferences.getObject("object_value", Player.class);
        return current;
    }



}
