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

function printRequestHeaders(req) {
    console.log("\nReceived headers");
    console.log("----------------");

    for (var key in req.headers) {
        console.log(key + ": " + req.headers[key]);
    }
}

function printRequestParameters(req) {
    console.log("\nReceived Parameters");
    console.log("-------------------");

    for (var key in req.body) {
        console.log(key + ": " + req.body[key]);
    }

    if (Object.keys(req.body).length === 0) console.log("no parameters\n");
}

// handle multipart uploads
app.post('/upload/multipart', multerFiles, function(req, res) {
    console.log("\n\nHTTP/Multipart Upload Request from: " + req.ip);
    printRequestHeaders(req);
    printRequestParameters(req);

    if (fileUploadCompleted) {
        fileUploadCompleted = false;
        res.header('transfer-encoding', ''); // disable chunked transfer encoding
        res.end("Upload Ok!");
    }
});

// handle binary uploads
app.post('/upload/binary', function(req, res) {
    console.log("\n\nBinary Upload Request from: " + req.ip);
    printRequestHeaders(req);

    var filename = req.headers["file-name"];
    console.log("Started binary upload of: " + filename);
    var filepath = path.resolve(UPLOAD_PATH, filename);
    var out = fs.createWriteStream(filepath, { flags: 'w', encoding: null, fd: null, mode: 666 });
    req.pipe(out);
    req.on('end', function() {
        console.log("Finished binary upload of: " + filename + " to: " + filepath);
        res.sendStatus(200);
    });
});

var server = app.listen(SERVER_PORT, function() {
    console.log("Web server started. Listening on " +
                server.address().address + ":" + server.address().port);
    console.log("\nThe following endpoints are available for upload testing:\n" +
                "HTTP/Multipart: http://YOUR_LOCAL_IP:3000/upload/multipart\n" +
                "Binary:         http://YOUR_LOCAL_IP:3000/upload/binary\n");
});
