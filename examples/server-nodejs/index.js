var express = require('express');
var multer = require('multer');
var fs = require('fs');
var path = require('path');
var os = require('os');
var passport = require('passport');
var HttpBasicAuth = require('passport-http').BasicStrategy;
var app = express();

var UPLOAD_PATH = "./uploads/";
var SERVER_PORT = 3000;

var basicAuthUser = {
    username: "test",
    password: "pass"
};

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
    return "HTTP/Multipart:              http://" + ipAddress + ":" + SERVER_PORT + "/upload/multipart\n" +
           "HTTP/Multipart (Basic Auth): http://" + ipAddress + ":" + SERVER_PORT + "/upload/multipart-ba\n" +
           "Binary:                      http://" + ipAddress + ":" + SERVER_PORT + "/upload/binary\n" +
           "Binary (Basic Auth):         http://" + ipAddress + ":" + SERVER_PORT + "/upload/binary-ba\n" +
           "401 Forbidden:               http://" + ipAddress + ":" + SERVER_PORT + "/upload/forbidden\n"
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

// configure passport for Basic Auth strategy
passport.use('basic-admin', new HttpBasicAuth({ realm: 'Upload Service' },
    function(username, password, done) {
        if (username === basicAuthUser.username &&
            password === basicAuthUser.password)  {
            return done(null, basicAuthUser);
        }
        return done(null, false);
    }
));

app.use(passport.initialize());
var useBasicAuth = passport.authenticate('basic-admin', { session: false });

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
                    file.originalname + "\n  mime type: " + file.mimetype);
    },

    onFileUploadComplete: function (file) {
        var fullPath = path.resolve(UPLOAD_PATH, file.originalname);
        console.log("Completed file upload\n  parameter name: " +
                    file.fieldname + "\n  file name: " +
                    file.originalname + "\n  mime type: " + file.mimetype +
                    "\n  in: " + fullPath);
        fileUploadCompleted = true;
    }
});

app.get('/', function(req, res) {
    res.end("Android Upload Service Demo node.js server running!");
});

var multipartUploadHandler = function(req, res) {
    if (fileUploadCompleted) {
        fileUploadCompleted = false;
        res.header('transfer-encoding', ''); // disable chunked transfer encoding
        res.end("Upload Ok!");
    }
};

var binaryUploadHandler = function(req, res) {
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
};

// handle multipart uploads
app.post('/upload/multipart', multipartReqInterceptor, multerFiles, multipartUploadHandler);
app.post('/upload/multipart-ba', useBasicAuth, multipartReqInterceptor, multerFiles, multipartUploadHandler);

// handle binary uploads
app.post('/upload/binary', binaryUploadHandler);
app.post('/upload/binary-ba', useBasicAuth,binaryUploadHandler);

// endpoint which returns always 401 and a JSON response in the body
app.post('/upload/forbidden', function(req, res) {
    res.status(401);
    res.json({
        success: false,
        message: "this endpoint always returns 401! It's for testing only"
    });
});

var server = app.listen(SERVER_PORT, function() {
    console.log("Web server started. Listening on all interfaces on port " +
                server.address().port);

    console.log("\nThe following endpoints are available for upload testing:\n");
    printAvailableEndpoints();

    console.log("Basic auth credentials are: " + JSON.stringify(basicAuthUser));
});
