const express = require('express');
const app = express();

app.use(express.urlencoded({ extended: true }));
app.use(express.json());

module.exports = app;

app.listen(process.env.PORT || 3001, (err) => {
    if (err) throw err
    console.log('Server running in http://127.0.0.1:3001')
});

app.get("/advertiser-service/api/crosssells", (req, res, next) => {
    res.json(
        [
            {
                name: "Ray-Ban Stories: Wayfair Shiny Black Frame",
                brand: "Ray-Ban",
                price: 239.00
            },
            {
                name: "Ray-Ban Aviator Classic",
                brand: "Ray-Ban",
                price: 70.50
            },
            {
                name: "Oakley Men's Thinklink",
                brand: "Oakly",
                price: 67.00
            },
        ]
    );
    console.log("returned data from crosssells call");
});

app.post("/advertiser-service/api/impression/ack", (req, res, next) => {
    res.json(
        {response: "Ok"}
    );
    console.log("acknowledged impression");
});

app.post("/advertiser-service/api/availability/update", (req, res, next) => {
    res.json(
        {response: "Ok"}
    );
    console.log("updated availability");
});