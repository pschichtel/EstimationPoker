let interval = -1;
let socket = new WebSocket(`ws://${window.document.location.host}/ws`);
socket.addEventListener('open', e => {
    console.log(e);
    interval = setInterval(() => socket.send("test" + Math.random()), 1000);
});
socket.addEventListener('message', e => {
    console.log(e);
});
socket.addEventListener("error", e => {
    console.log(e);
});
socket.addEventListener('close', e => {
    console.log(e);
    if (interval !== -1) {
        clearInterval(interval);
        interval = -1;
    }
});

