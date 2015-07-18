var express = require('express');
var multer = require('multer');
var app = express();

var UPLOAD_PATH = "./uploads/";
var SERVER_PORT = 3000;

// configure multer for upload management
var fileUploadCompleted = false;
var multerFiles = multer({ dest: UPLOAD_PATH,
    rename: function (fieldname, filename) {
        return filename;
    },
    onFileUploadStart: function (file) {
        console.log("Started upload of: " + file.originalname);
    },
    onFileUploadComplete: function (file) {
        console.log("Finished upload of: " + file.fieldname + " to: " + file.path);
        fileUploadCompleted = true;
    }
});

app.get('/', function(req, res) {
    res.end("Android Upload Service Demo node.js server running!");
});

// handle uploads
app.post('/upload', multerFiles, function(req, res) {
    if(fileUploadCompleted){
        fileUploadCompleted = false;
        res.header('transfer-encoding', ''); // disable chunked transfer encoding
        res.end("Upload Ok!");
    }
});

var server = app.listen(SERVER_PORT, function() {
    console.log("Web server started. Listening on " +
                server.address().address + ":" + server.address().port);
});
