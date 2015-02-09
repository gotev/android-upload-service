package com.alexbbb.uploadservice.demo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.FileToUpload;
import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.UploadService;
import com.alexbbb.uploadservice.demo.adapters.ImageGridSelectableAdapter;
import com.alexbbb.uploadservice.demo.adapters.UploadRequestAdapter;

/**
 * Activity that demonstrates how to use Android Upload Service.
 * 
 * @author Alex Gotev
 * 
 */
public class MainActivity extends Activity implements  FileProgressListener, RequestProgressListener{

	private static final String TAG = "AndroidUploadServiceDemo";
	private static final int PICK_IMAGE = 4 + 8 + 15 + 16 + 23 + 42;
	private ProgressBar progressBar;
	private Button uploadButton;
	private EditText serverUrl;
	private EditText fileToUpload;
	private EditText parameterName;
	private ArrayList<String> files = new ArrayList<String>();
	ArrayList<UploadRequest> unprocessedRequests = new ArrayList<UploadRequest>();
	ArrayList<UploadRequest> requestHistory = new ArrayList<UploadRequest>();
	String url = "http://192.168.199.105:12345/upload.php";

	String param = "uploaded_file";


	private Button chooseButton;
	private TextView fileProgress;
	private ProgressBar fileProgressBar;
	private Tests tests;
	private TextView tvUploadId;
	private TextView tvFileName;
	private ListView listView;
	private UploadRequestAdapter uploadRequestAdapter;
	private GridView gridView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		App.setMainActivity(this);

		// Set your application namespace to avoid conflicts with other apps
		// using this library
		UploadService.NAMESPACE = "com.alexbbb";
		tests = new Tests(this, url, param);

		progressBar = (ProgressBar) findViewById(R.id.pbar_request);
		fileProgressBar = (ProgressBar) findViewById(R.id.pbar_file);
		tvFileName = (TextView)findViewById(R.id.tv_file_name);
		tvUploadId = (TextView)findViewById(R.id.tv_request_id);
		uploadButton = (Button) findViewById(R.id.uploadButton);
		chooseButton = (Button) findViewById(R.id.btn_create_task);
		listView = (ListView) findViewById(R.id.listView);
		gridView = (GridView) findViewById(R.id.gridView);
		final CheckBox chkImmediate = (CheckBox) findViewById(R.id.chk_immediate_upload);
		uploadRequestAdapter = new UploadRequestAdapter(this, requestHistory);
		listView.setAdapter(uploadRequestAdapter);

		try {
			final ImageGridSelectableAdapter imageGridSelectableAdapter = new ImageGridSelectableAdapter(this, tests.createImagesFromAssets());
			gridView.setAdapter(imageGridSelectableAdapter);
			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
					UploadRequest request;

					if (unprocessedRequests.isEmpty()){
						request = createEmptyRequest();
					}else{
						if (chkImmediate.isChecked()){
							request = createEmptyRequest();
						}else {
							request = unprocessedRequests.get(unprocessedRequests.size() - 1);
						}
					}
					request.addFileToUpload(
							(String)imageGridSelectableAdapter.getItem(i),
							param ,
							(String)imageGridSelectableAdapter.getItem(i),
							ContentType.APPLICATION_OCTET_STREAM
							);
					uploadRequestAdapter.notifyDataSetChanged();
					if(chkImmediate.isChecked()){
						try {
							UploadService.startUpload(request);
							unprocessedRequests.clear();
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}

				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}


		uploadButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				tests.uploadRequests(unprocessedRequests);
				unprocessedRequests.clear();
			}
		});

		chooseButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				try {
				//	unprocessedRequests.add(tests.createRequestFromImages(new ArrayList<String>()));
				//	UploadRequest request = tests.createRequestFromImages(tests.createImagesFromAssets());
					//requestHistory.add(request);
					UploadRequest request = createEmptyRequest();
					uploadRequestAdapter.notifyDataSetChanged();
					log("NEW TASK no." + unprocessedRequests.size() + " created with uploadId " + request.getUploadId() + " and " + request.getFilesToUpload().size() + " images");
					log("	no. of  unprocessedRequests pending : " + unprocessedRequests.size());
				} catch (Exception e) {
					e.printStackTrace();
					log("ERROR occured trying to create task : " + e.getMessage() );
				}

			}
		});

		progressBar.setMax(100);
		progressBar.setProgress(0);

		fileProgressBar.setMax(100);
		fileProgressBar.setProgress(0);

	}

	private UploadRequest createEmptyRequest(){

		UploadRequest uploadRequest = tests.createEmptyRequest();
		unprocessedRequests.add(uploadRequest);
		requestHistory.add(0,uploadRequest);
		return  uploadRequest;
	}

	@Override
	protected void onResume() {
		super.onResume();
		tests.registerServiceReceiver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		tests.unRegisterServiceReceiver(this);
	}

	@Override
	public void setFileProgress(String uploadId, String fileId , int progress) {
		fileProgressBar.setProgress(progress);
		tvFileName.setText(fileId);

		for (UploadRequest request : requestHistory){
			if (request.getUploadId().equals(uploadId)){
				for (FileToUpload fileToUpload1 : request.getFilesToUpload()){
					if (fileToUpload1.getFileId().equals(fileId)){
						fileToUpload1.setProgress(progress);
						uploadRequestAdapter.notifyDataSetChanged();
					}
				}
			}
		}



	}


	@Override
	public void setRequestProgress(String uploadId, int progress){
		progressBar.setProgress(progress);
		tvUploadId.setText(uploadId);
	}


	public void log(String message){
		((TextView)findViewById(R.id.tv_log)).append("\n" + message);
		((ScrollView)((TextView) findViewById(R.id.tv_log)).getParent()).fullScroll(ScrollView.FOCUS_DOWN);
		Log.e(TAG, message);
	}
}
