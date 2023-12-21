var interv;
interv = setInterval(() => {
    const txt = document.getElementById("txt");
    if (txt && !txt.innerText.startsWith("Loading")) {
        browser.runtime.sendNativeMessage("browser", txt.innerText);
        clearInterval(interv);
    }
},500)
