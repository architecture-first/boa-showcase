const express = require('express');
const app = express();

const redis = require('redis');

const setupRedis = () => {
    const url = `redis://${process.env.REDIS_HOST || 'localhost'}:${process.env.REDIS_PORT || 6379}`;
    const client = redis.createClient({
        url: url,
        password: process.env.redisPwd || ''
    });
    client.connect();
    client.on('error', err => console.log('Redis error: ', err.message));
    client.on('connect', () => console.log('Connected to redis server'));

    return client;
}
let redisClient = setupRedis();

const processPublishedMessages = () => {
    redisClient.blPop("Hub-messages",0).then(
        (data, item) => {
            if (data) {
                console.log("data=" + data);
                let payload = JSON.parse(data.element);
                let ws = webSockets[payload["boa-conn"]];
                if (ws) {
                    ws.send(data.element)
                }
            }
            else {
                console.log("no data found for Hub-message")
            }
            process.nextTick(processPublishedMessages);
        }
    );
}

app.use(express.urlencoded({ extended: true }));
app.use(express.json());

const path = require('path');
const proxy = require('express-http-proxy');
const port = process.env.PORT || 3010;
const rootPath = process.env.ROOT_PATH || __dirname;
const proxyUrl = process.env.PROXY_URL || 'localhost:8084';

console.log(`hub: port: ${port}`);
console.log(`hub: path: ${rootPath}`);
console.log(`hub: dirname: ${__dirname}`);
console.log(`hub: proxyUrl: ${proxyUrl}`);
var wss = new (require('ws')).Server({noServer: true}),
    webSockets = {} // userID: webSocket
var serverWebSockets = {}

wss.on('connection', function (webSocket, request) {
    var connId = request.url.split('/').filter(Boolean).pop();
    if (!connId || connId == "undefined") {
        console.log("ERROR: Socket request: " + request.url + " is invalid");
        webSocket.terminate();
        return;
    }

    if (request.url.includes("publisher")) {
        connId = "publisher/" + connId;
    }
    webSockets[connId] = webSocket
    console.log('connected: ' + connId + ' in ' + Object.getOwnPropertyNames(webSockets));

    let bridgeMessage = {
        connected: true
    }
    console.log('sent to ' + bridgeMessage.connId + ': ' + JSON.stringify(bridgeMessage))
    bridgeMessage.connId = connId
    webSocket.send(JSON.stringify(bridgeMessage));

    webSocket.on('message', function(message) {
        console.log('received from ' + connId + ': ' + message)
        let bridgeMessage = JSON.parse(message)
    });

    webSocket.on('close', function () {
        delete webSockets[connId]
        console.log('deleted: ' + connId)
    });
});

console.log(`hub: proxyUrl: ${proxyUrl}`);
app.use('/images', (req, res) => {
    const imagePath = path.join(rootPath, '/images/' + req.url);
    console.log(`hub: imagePath: ${imagePath}`);
    res.sendFile('images/' + req.url, {root: rootPath});
});

/*
app.post('/hub/customer/suggested-products', (req, res) => {
    let connId = req.header("boa-conn");
    let websocket = webSockets[connId];
    if (websocket) {
        webSocket.send(JSON.stringify(req.body));
    }
    res.json(
        {response: "Ok"}
    );
    console.log("received request");
});
*/

app.options('*', (res, req) => {
    res.header('Access-Control-Allow-Methods', 'GET, PATCH, PUT, POST, DELETE, OPTIONS')
})

//req.baseUrl + (queryString ? '?' + queryString : '')
const apiProxy = proxy(proxyUrl, {
    proxyReqPathResolver: req => {
        var parts = req.url.split('?');
        var queryString = parts[1];
        return req.baseUrl  + (queryString ? '?' + queryString : '');
    }
});

app.use('/api/*', apiProxy);

app.get('/', (req, res) => {
    const indexPath = path.join(rootPath, '/index.html');
    console.log(`hub: index: ${indexPath}`);
    res.sendFile('index.html', {root: rootPath} );
});

const server = app.listen(port);
server.on('upgrade', (request, socket, head) => {
    wss.handleUpgrade(request, socket, head, socket => {
        wss.emit('connection', socket, request);
    });
});

console.log('Server started at http://localhost:' + port);

(function main() {
    processPublishedMessages();
})();

