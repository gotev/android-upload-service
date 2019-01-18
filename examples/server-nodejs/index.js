const http = require("http");
const express = require('express');
const PORT = process.env.PORT || 5000;
const app = express();
const expressValidator = require('express-validator');
const expFileUpload = require("express-fileupload");
const bodyParser = require('body-parser');
const methodOverride = require('method-override');
const cookieParser = require('cookie-parser');
const session = require('express-session');

app.use(expressValidator());
app.use(expFileUpload());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(cookieParser());
app.use(session({secret: 'CSELL', saveUninitialized: false, resave: false}));

app.post('/image', (req, res) => {

    const fileKeys = Object.keys(req.files);

    if (Object.keys(req.files).length == 0){

        res.json(returnJSONData(400, 'No files', true, "Error"));
    }

    fileKeys.forEach((value)=>{

        let sampleFile = req.files[value];
        const extension = path.extname(sampleFile.name);

        sampleFile.mv(path.dirname(require.main.filename)+'/public/uploads/users/CSELL_'
            +randomNumber(4)+"_"+moment().valueOf()+extension, (err)=> {

            if(err){

                res.json(returnJSONData(200, 'File name', false, returnArray));
            }
        });

        returnArray.push(sampleFile);
    });

    res.json(returnJSONData(200, 'File name', false, returnArray));
});

const randomNumber = (length)=>{

    let add = 1, max = 12 - add;

    if ( length > max ) {
        return generate(max) + generate(length - max);
    }

    max        = Math.pow(10, length+add);
    let min    = max/10;
    let number = Math.floor( Math.random() * (max - min + 1) ) + min;

    return ("" + number).substring(add);
};

const returnJSONData = (httpCode, message, error, data) => {

    return [{code:httpCode, message:message, error:error, value:data}];
};
