package dev.mfm.renamer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import dev.mfm.app.databinding.ActivityMainBinding;
import java.io.File;

public class MainActivity extends AppCompatActivity {

  private ActivityMainBinding binding;
  private static final int REQUEST_CODE_PERMISSION = 100;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    binding.browseButton.setOnClickListener(view -> requestPermissionsIfNecessary());

    binding.renameButton.setOnClickListener(view -> startRenaming());
  }

  private void requestPermissionsIfNecessary() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:" + getPackageName()));
        storagePermissionLauncher.launch(intent);
      } else {
        openDirectoryPicker();
      }
    } else {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, 
          new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
      } else {
        openDirectoryPicker();
      }
    }
  }

  private final ActivityResultLauncher<Intent> storagePermissionLauncher = 
    registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
      if (Environment.isExternalStorageManager()) {
        openDirectoryPicker();
      } else {
        Toast.makeText(this, "Permission required to access storage", Toast.LENGTH_SHORT).show();
      }
    });

  private void openDirectoryPicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    directoryPickerLauncher.launch(intent);
  }

  private final ActivityResultLauncher<Intent> directoryPickerLauncher =
    registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
      if (result.getResultCode() == RESULT_OK && result.getData() != null) {
        Uri uri = result.getData().getData();
        binding.directoryEditText.setText(uri.getPath());
      }
    });

  private void startRenaming() {
    String directoryPath = binding.directoryEditText.getText().toString().trim();
    String baseName = binding.baseNameEditText.getText().toString().trim();

    if (directoryPath.isEmpty() || baseName.isEmpty()) {
      Toast.makeText(this, "Please provide both a directory and a base name.", Toast.LENGTH_SHORT).show();
      return;
    }

    File directory = new File(directoryPath);
    if (!directory.exists() || !directory.isDirectory()) {
      Toast.makeText(this, "Invalid directory selected.", Toast.LENGTH_SHORT).show();
      return;
    }

    renameFiles(directory, baseName);
  }

  private void renameFiles(File directory, String baseName) {
    File[] files = directory.listFiles();
    if (files != null) {
      int index = 1;
      for (File file : files) {
        if (file.isFile()) {
          String extension = file.getName().substring(file.getName().lastIndexOf("."));
          String newFileName = baseName + "-" + index + extension;
          File newFile = new File(directory, newFileName);
          boolean renamed = file.renameTo(newFile);
          if (!renamed) {
            Toast.makeText(this, "Error renaming file: " + file.getName(), Toast.LENGTH_SHORT).show();
          }
          index++;
        }
      }
      Toast.makeText(this, "Files renamed successfully!", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CODE_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        openDirectoryPicker();
      } else {
        Toast.makeText(this, "Permission required to access storage", Toast.LENGTH_SHORT).show();
      }
    }
  }
}