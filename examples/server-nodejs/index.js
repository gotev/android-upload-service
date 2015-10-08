var express = require('express');
var multer = require('multer');
var fs = require('fs');
var path = require('path');

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
        console.log("Started multipart upload of: " + file.originalname);
    },
    onFileUploadComplete: function (file) {
        console.log("Finished multipart upload of: " + file.fieldname + " to: " + file.path);
        fileUploadCompleted = true;
    }
});

app.get('/', function(req, res) {
    res.end("Android Upload Service Demo node.js server running!");
});

// handle multipart uploads
app.post('/upload/multipart', multerFiles, function(req, res) {
    if (fileUploadCompleted) {
        fileUploadCompleted = false;
        res.header('transfer-encoding', ''); // disable chunked transfer encoding
        res.end("Upload Ok!");
    }
});

// handle binary uploads
app.post('/upload/binary', function(req, res) {
    var filename = req.headers["file-name"];
    console.log("Started binary upload of: " + filename);
    var filepath = path.resolve(UPLOAD_PATH, filename);
    var out = fs.createWriteStream(filepath, { flags: 'w', encoding: null, fd: null, mode: 666 });
    req.pipe(out);
    req.on('end', function() {
        console.log("Finished multipart upload of: " + filename + " to: " + filepath);
        res.sendStatus(200);
    });
});

var server = app.listen(SERVER_PORT, function() {
    console.log("Web server started. Listening on " +
                server.address().address + ":" + server.address().port);
});
