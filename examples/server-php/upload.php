<?php
    if ($_FILES["uploaded_file"]["error"] > 0) {
        echo "Error: " . $_FILES["uploaded_file"]["error"] . "<br>";
    } else {
        echo "Upload: " . $_FILES["uploaded_file"]["name"] . "<br>";
        echo "Type: " . $_FILES["uploaded_file"]["type"] . "<br>";
        echo "Size: " . ($_FILES["uploaded_file"]["size"] / 1024) . " kB<br>";
        echo "Stored in: " . $_FILES["uploaded_file"]["tmp_name"];
    }
?>