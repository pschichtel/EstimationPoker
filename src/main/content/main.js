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

function bindSelectionScreen(selectionRoot) {

}

function bindMasterScreen(masterRoot) {

}

function bindPlayerScreen(playerRoot) {


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

            alert(`Jersey clicked! Size: ${relative}, Story points: ${absolute}`)
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


window.addEventListener("DOMContentLoaded", e => {

    const selectionScreen = document.getElementById('selection');
    const masterScreen = document.getElementById('master');
    const playerScreen = document.getElementById('player');

    bindSelectionScreen(selectionScreen);
    bindMasterScreen(masterScreen);
    bindPlayerScreen(playerScreen);
});
