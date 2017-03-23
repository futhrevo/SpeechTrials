package in.hedera.reku.speechtrial;

import android.app.SearchManager;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class VoiceSearchActivity extends AppCompatActivity {
    private static final String TAG = VoiceSearchActivity.class.getSimpleName();
    private VoiceInteractor voiceInteractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_voice_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        if(intent.hasExtra(SearchManager.QUERY)){
            Log.d(TAG, "Extra has " + intent.getStringExtra(SearchManager.QUERY));
        }
        if(isVoiceInteraction()){
            Log.d(TAG, "Voice Interaction started");
            voiceInteractor = getVoiceInteractor();
            VoiceInteractor.PickOptionRequest.Option option = new VoiceInteractor.PickOptionRequest.Option("cheese",1);
            option.addSynonym("ready");
            option.addSynonym("go");
            option.addSynonym("take it");
            option.addSynonym("ok");
            VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt("Say Cheese");

            voiceInteractor.submitRequest(new VoiceInteractor.PickOptionRequest(prompt, new VoiceInteractor.PickOptionRequest.Option[]{option}, null) {
                @Override
                public void onPickOptionResult(boolean finished, Option[] selections, Bundle result) {
                    if (finished && selections.length == 1) {
                        Message message = Message.obtain();
                        message.obj = result;
                        Log.d(TAG, "Voice Interactor success");
                    } else {
                        getActivity().finish();
                        Log.d(TAG, "Voice Interactor teardown");
                    }
                }
                @Override
                public void onCancel() {
                    getActivity().finish();
                    Log.d(TAG, "Voice Interactor onCancel");
                }
            });
        }else{
            Log.d(TAG, "not a Voice Interaction");
        }
        if (isVoiceInteractionRoot()) {  //started by voice only?  I think. doc's are not clear here.
            Log.v(TAG, "Intent is " + intent.getAction());
            Log.v(TAG, "it's working!?");
        }
    }

}
