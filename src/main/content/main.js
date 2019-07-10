
class PendingCall {
    constructor(resolve, reject) {
        this.resolve = resolve;
        this.reject = reject;
    }
}

class Connection {
    constructor() {
        this.pendingCalls = new Map();
        this.messageCounter = 0;
        this.socket = new WebSocket(`ws://${window.document.location.host}/ws`);
        this.socket.addEventListener('open', e => {
            console.log(e);
        });
        this.socket.addEventListener('message', e => {
            let data = null;
            try {
                data = JSON.parse(e.data);
            } catch (e) {
                console.log("Error while parsing response JSON");
                console.log("data", e.data);
                console.log("error", e);
            }

            if (data) {
                if ('messageId' in data) {
                    let pendingCall = this.pendingCalls.get(data.messageId);
                    pendingCall.resolve(data.payload);
                }
            }
        });
        this.socket.addEventListener("error", e => {
            console.log(e);
            this.dropAllPending();
        });
        this.socket.addEventListener('close', e => {
            console.log(e);
            this.dropAllPending();
        });
    }

    dropAllPending() {
        this.pendingCalls.forEach((value) => {
            value.reject("error")
        });
        this.pendingCalls.clear();
    }

    send(payload) {
        return new Promise((resolve, reject) => {
            let id = this.messageCounter++;
            this.pendingCalls.set(id, new PendingCall(resolve, reject));
            let msg = {
                messageId: id,
                type: 'request',
                payload: payload,
            };

            this.socket.send(JSON.stringify(msg));
        });
    }
}

function hide(elem) {
    elem.classList.add('hidden');
}

function show(elem) {
    elem.classList.remove('hidden');
}

function bindSelectionScreen(selectionRoot, service) {

    show(selectionRoot);

    const playerScreen = document.getElementById('player');
    const masterScreen = document.getElementById('master');

    const masterButton = document.getElementById('enter-master');
    const playerButton = document.getElementById('enter-player');

    masterButton.addEventListener('click', e => {
        e.preventDefault();
        hide(selectionRoot);
        bindMasterScreen(masterScreen, service);
    }, {once: true});

    playerButton.addEventListener('click', e => {
        e.preventDefault();
        hide(selectionRoot);
        bindPlayerScreen(playerScreen, service);
    }, {once: true});

}

function bindMasterScreen(masterRoot, service) {

    show(masterRoot);

}

function bindPlayerScreen(playerRoot, service) {

    show(playerRoot);

    let selectedJersey = null;


    function createCard(relative, absolute) {
        let card = document.createElement('div');
        card.classList.add('card');

        let jersey = document.createElement('img');
        jersey.classList.add('jersey');
        jersey.src = 'jersey.svg';
        jersey.alt = 'Football jersey to represent effort';
        card.appendChild(jersey);

        let relativeDisplay = document.createElement('span');
        relativeDisplay.classList.add('relative');
        relativeDisplay.appendChild(document.createTextNode(relative));
        card.appendChild(relativeDisplay);

        let absoluteDisplay = document.createElement('span');
        absoluteDisplay.classList.add('absolute');
        absoluteDisplay.appendChild(document.createTextNode(absolute));
        card.appendChild(absoluteDisplay);

        card.addEventListener('click', e => {
            e.preventDefault();

            if (selectedJersey) {
                selectedJersey.classList.toggle('active');
            }
            card.classList.toggle('active');
            selectedJersey = card;

            console.log(`Jersey clicked! Size: ${relative}, Story points: ${absolute}`);
            service.send({relative: relative, absolute: absolute});
        });

        return card;
    }

    const sizes = [
        ['XXS',   1],
        ['XS',    2],
        ['S',     3],
        ['M',     5],
        ['L',     8],
        ['XL',   13],
        ['XXL',  21],
        ['XXXL', 34],
        ['∞',    Infinity]
    ];

    const cardContainer = playerRoot.querySelector("#card-container");

    for ([rel, abs] of sizes) {
        let card = createCard(rel, abs);
        cardContainer.appendChild(card);
    }

}

window.addEventListener('hashchange', () => {
    if (window.innerDocClick) {
        //Your own in-page mechanism triggered the hash change
    } else {
        //Browser back or forward button was pressed
    }
});

window.addEventListener("DOMContentLoaded", () => {

    const noJS = document.getElementById('no-javascript');
    hide(noJS);

    const selectionScreen = document.getElementById('selection');
    const service = new Connection();
    bindSelectionScreen(selectionScreen, service);
});
