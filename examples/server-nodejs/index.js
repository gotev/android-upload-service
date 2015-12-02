var express = require('express');
var multer = require('multer');
var fs = require('fs');
var path = require('path');
var os = require('os');
var app = express();

var UPLOAD_PATH = "./uploads/";
var SERVER_PORT = 3000;

function printRequestHeaders(req) {
    console.log("\nReceived headers");
    console.log("----------------");

    for (var key in req.headers) {
        console.log(key + ": " + req.headers[key]);
    }

    console.log("");
}

function printRequestParameters(req) {
    console.log("\nReceived Parameters");
    console.log("-------------------");

    for (var key in req.body) {
        console.log(key + ": " + req.body[key]);
    }

    if (Object.keys(req.body).length === 0)
        console.log("no text parameters\n");
    else
        console.log("");
}

function getEndpoints(ipAddress) {
    return "HTTP/Multipart: http://" + ipAddress + ":" + SERVER_PORT + "/upload/multipart\n" +
           "Binary:         http://" + ipAddress + ":" + SERVER_PORT + "/upload/binary\n";
}

function printAvailableEndpoints() {
    var ifaces = os.networkInterfaces();

    Object.keys(ifaces).forEach(function (ifname) {
        ifaces[ifname].forEach(function (iface) {
            // skip internal (i.e. 127.0.0.1) and non-ipv4 addresses
            if ('IPv4' !== iface.family || iface.internal !== false) {
                return;
            }

            console.log(getEndpoints(iface.address));
        });
    });
}

var multipartReqInterceptor = function(req, res, next) {
    console.log("\n\nHTTP/Multipart Upload Request from: " + req.ip);
    printRequestHeaders(req);

    next();
};

// configure multer for upload management
var fileUploadCompleted = false;
var multerFiles = multer({
    dest: UPLOAD_PATH,
    rename: function (fieldname, filename) {
        return filename;
    },

    onParseEnd: function(req, next) {
        printRequestParameters(req);

        next();
    },

    onFileUploadStart: function (file) {
        console.log("Started file upload\n  parameter name: " +
                    file.fieldname + "\n  file name: " +
                    file.originalname + "\n");
    },

    onFileUploadComplete: function (file) {
        var fullPath = path.resolve(UPLOAD_PATH, file.originalname);
        console.log("Completed file upload\n  parameter name: " +
                    file.fieldname + "\n  file name: " +
                    file.originalname + "\n  in: " + fullPath);
        fileUploadCompleted = true;
    }
});

app.get('/', function(req, res) {
    res.end("Android Upload Service Demo node.js server running!");
});

// handle multipart uploads
app.post('/upload/multipart', multipartReqInterceptor, multerFiles, function(req, res) {
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
    var out = fs.createWriteStream(filepath, { flags: 'w', encoding: 'binary', fd: null, mode: '644' });
    req.pipe(out);
    req.on('end', function() {
        console.log("Finished binary upload of: " + filename + "\n  in: " + filepath);
        res.sendStatus(200);
    });
});

var server = app.listen(SERVER_PORT, function() {
    console.log("Web server started. Listening on all interfaces on port " +
                server.address().port);

    console.log("\nThe following endpoints are available for upload testing:\n");
    printAvailableEndpoints();
});
