
/*
 * =====================================================
 * Remote Pulse
 * script.js
 * =====================================================
 */

let selectedSlot = null;
let currentTiles = [];
let installedApps = [];
let selectedInstalledApp = null;


/*
 * =====================================================
 * Server Information
 * =====================================================
 */

async function loadServerInfo() {
    try {
        const response = await fetch("/api/server-info");

        if (!response.ok) {
            throw new Error("Server information request failed.");
        }

        const serverInfo = await response.json();

        document.getElementById("server-address").textContent =
            `Server - ${serverInfo.address}`;

    } catch (error) {
        console.error("Failed to load server information:", error);

        document.getElementById("server-address").textContent =
            "Server - Unknown";
    }
}


/*
 * =====================================================
 * Tiles
 * =====================================================
 */

async function loadTiles() {
    console.log(">>> loadTiles() CALLED");

    try {
        const response = await fetch(
            `/api/tiles?t=${Date.now()}`,
            { cache: "no-store" }
        );

        if (!response.ok) {
            throw new Error("Failed to retrieve tiles.");
        }

        currentTiles = await response.json();

        console.log(">>> TILES RECEIVED:", currentTiles);

        displayTiles(currentTiles);

    } catch (error) {
        console.error("Failed to load tiles:", error);
    }
}


function displayTiles(tiles) {
    const container = document.getElementById("tile-container");

    container.innerHTML = "";

    tiles.sort((a, b) => a.slotNumber - b.slotNumber);

    tiles.forEach(tile => {
        const tileElement = document.createElement("div");

        tileElement.className = "tile";

        tileElement.addEventListener("click", () => {
            openConfiguration(tile.slotNumber);
        });

        const isEmpty =
            !tile.name &&
            !tile.type &&
            !tile.target &&
            !tile.icon;

        if (isEmpty) {
            tileElement.classList.add("empty-tile");

            const questionMark = document.createElement("div");

            questionMark.className = "empty-tile-question";
            questionMark.textContent = "?";

            tileElement.appendChild(questionMark);

        } else if (tile.icon) {
            const icon = document.createElement("img");

            icon.className = "tile-icon";

            let iconUrl;

            if (tile.icon.startsWith("system-icons/")) {
                iconUrl = `/${tile.icon}?v=${Date.now()}`;
            } else {
                iconUrl =
                    `/icons/${tile.icon}?v=${Date.now()}-${Math.random()}`;
            }

            icon.onload = function () {
                console.log(
                    `ICON LOADED FOR SLOT ${tile.slotNumber}:`,
                    tile.name
                );
            };

            icon.onerror = function () {
                console.error(
                    `ICON FAILED FOR SLOT ${tile.slotNumber}:`,
                    iconUrl
                );
            };

            icon.src = iconUrl;
            icon.alt = tile.name || `Slot ${tile.slotNumber}`;

            tileElement.appendChild(icon);

        } else {
            const questionMark = document.createElement("div");

            questionMark.className = "empty-tile-question";
            questionMark.textContent = "?";

            tileElement.appendChild(questionMark);
        }

        container.appendChild(tileElement);
    });
}


/*
 * =====================================================
 * Configuration Modal
 * =====================================================
 */

function openConfiguration(slotNumber) {
    selectedSlot = slotNumber;

    const tile = currentTiles.find(
        item => item.slotNumber === slotNumber
    );

    document.getElementById("config-slot").textContent =
        `Slot ${slotNumber}`;

    const deleteButton = document.getElementById(
        "delete-configuration-button"
    );

    const isConfigured =
        tile &&
        (tile.name || tile.type || tile.target || tile.icon);

    deleteButton.classList.toggle("hidden", !isConfigured);

    document.getElementById("config-modal").classList.remove("hidden");
}


function closeConfiguration() {
    document.getElementById("config-modal").classList.add("hidden");
    selectedSlot = null;
}


/*
 * =====================================================
 * System Button
 * =====================================================
 */

function openSystemButton() {
    if (selectedSlot === null) return;

    document.getElementById("system-button-slot").textContent =
        `Slot ${selectedSlot}`;

    document.getElementById("config-modal").classList.add("hidden");
    document.getElementById("system-button-modal").classList.remove("hidden");
}


async function saveSystemButton(action) {
    if (selectedSlot === null) return;

    try {
        const response = await fetch("/api/system-button/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                slotNumber: selectedSlot,
                action: action
            })
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        await loadTiles();

        document.getElementById("system-button-modal")
            .classList.add("hidden");

        selectedSlot = null;

    } catch (error) {
        console.error("Failed to save system button:", error);
        alert("Failed to save system button.");
    }
}


function closeSystemButton() {
    document.getElementById("system-button-modal")
        .classList.add("hidden");

    selectedSlot = null;
}


/*
 * =====================================================
 * Delete Configuration
 * =====================================================
 */

function openDeleteConfiguration() {
    if (selectedSlot === null) return;

    const tile = currentTiles.find(
        item => item.slotNumber === selectedSlot
    );

    if (!tile) {
        console.error("Selected tile could not be found.");
        return;
    }

    let displayName;

    if (tile.type === "WEBSITE" && tile.target) {
        displayName = tile.target;

    } else if (
        tile.type === "INSTALLED_APP" &&
        tile.name
    ) {
        displayName = tile.name;

    } else if (
        tile.type === "CUSTOM_APP" &&
        tile.name
    ) {
        displayName = tile.name;

    } else if (tile.name) {
        displayName = tile.name;

    } else if (tile.icon) {
        displayName = `Icon for Slot ${tile.slotNumber}`;

    } else {
        displayName = `Slot ${tile.slotNumber}`;
    }

    document.getElementById("delete-configuration-name")
        .textContent = displayName;

    document.getElementById("config-modal").classList.add("hidden");

    document.getElementById("delete-configuration-modal")
        .classList.remove("hidden");
}


function cancelDeleteConfiguration() {
    document.getElementById("delete-configuration-modal")
        .classList.add("hidden");

    document.getElementById("config-modal")
        .classList.remove("hidden");
}


async function deleteConfiguration() {
    if (selectedSlot === null) return;

    const slotToDelete = selectedSlot;

    try {
        const response = await fetch(
            `/api/tiles/slot/${slotToDelete}/configuration`,
            { method: "DELETE" }
        );

        if (!response.ok) {
            throw new Error(await response.text());
        }

        await new Promise(resolve => setTimeout(resolve, 300));

        await loadTiles();

        document.getElementById("delete-configuration-modal")
            .classList.add("hidden");

        document.getElementById("config-modal")
            .classList.add("hidden");

        selectedSlot = null;

    } catch (error) {
        console.error("Failed to delete configuration:", error);
        alert("Failed to delete configuration.");
    }
}


/*
 * =====================================================
 * Shared Custom Icon Preview
 * =====================================================
 */

/*
 * Creates a preview image next to a file input.
 * This avoids needing separate preview HTML for every modal.
 */
function setupIconPreview(inputId) {
    const fileInput = document.getElementById(inputId);

    if (!fileInput) return;

    let preview = document.getElementById(`${inputId}-preview`);

    if (!preview) {
        preview = document.createElement("img");

        preview.id = `${inputId}-preview`;
        preview.className = "custom-icon-preview hidden";
        preview.alt = "Selected icon preview";

        fileInput.insertAdjacentElement("afterend", preview);
    }

    fileInput.addEventListener("change", () => {
        previewSelectedIcon(fileInput, preview);
    });
}


function previewSelectedIcon(fileInput, preview) {
    const file = fileInput.files[0];

    if (!file) {
        preview.classList.add("hidden");
        preview.removeAttribute("src");
        return;
    }

    if (!file.type.startsWith("image/")) {
        alert("Please select a valid image file.");

        fileInput.value = "";
        preview.classList.add("hidden");
        preview.removeAttribute("src");

        return;
    }

    const reader = new FileReader();

    reader.onload = function () {
        preview.src = reader.result;
        preview.classList.remove("hidden");
    };

    reader.onerror = function () {
        alert("Failed to preview the selected image.");
        preview.classList.add("hidden");
    };

    reader.readAsDataURL(file);
}


function resetIconPreview(inputId) {
    const fileInput = document.getElementById(inputId);

    if (!fileInput) return;

    fileInput.value = "";

    const preview = document.getElementById(`${inputId}-preview`);

    if (preview) {
        preview.classList.add("hidden");
        preview.removeAttribute("src");
    }
}


/*
 * =====================================================
 * Shared Custom Icon Upload
 * =====================================================
 */

function uploadCustomIcon(file) {
    return new Promise(resolve => {
        const reader = new FileReader();

        reader.onload = async function () {
            try {
                const response = await fetch("/api/custom-icon/save", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        slotNumber: selectedSlot,
                        imageBase64: reader.result
                    })
                });

                if (!response.ok) {
                    throw new Error(await response.text());
                }

                resolve(true);

            } catch (error) {
                console.error("Failed to save custom icon:", error);
                alert("Failed to save custom icon.");
                resolve(false);
            }
        };

        reader.onerror = function () {
            console.error("Failed to read icon file.");
            alert("Failed to read icon file.");
            resolve(false);
        };

        reader.readAsDataURL(file);
    });
}


/*
 * Save an optional icon selected in the currently
 * open application configuration modal.
 */
async function saveCustomIconIfSelected() {
    let fileInput;

    if (!document.getElementById("installed-app-modal")
        .classList.contains("hidden")) {

        fileInput = document.getElementById(
            "installed-app-custom-icon"
        );

    } else if (!document.getElementById("custom-app-modal")
        .classList.contains("hidden")) {

        fileInput = document.getElementById(
            "custom-app-custom-icon"
        );

    } else if (!document.getElementById("website-modal")
        .classList.contains("hidden")) {

        fileInput = document.getElementById(
            "website-custom-icon"
        );
    }

    if (!fileInput || fileInput.files.length === 0) {
        return true;
    }

    return await uploadCustomIcon(fileInput.files[0]);
}


/*
 * =====================================================
 * Independent Change Icon
 * =====================================================
 */

function openChangeIcon() {
    if (selectedSlot === null) return;

    document.getElementById("change-icon-slot").textContent =
        `Slot ${selectedSlot}`;

    resetIconPreview("change-icon-file");

    document.getElementById("config-modal").classList.add("hidden");

    document.getElementById("change-icon-modal")
        .classList.remove("hidden");
}


async function saveIndependentIcon() {
    if (selectedSlot === null) return;

    const fileInput = document.getElementById("change-icon-file");

    if (fileInput.files.length === 0) {
        alert("Please select an icon first.");
        return;
    }

    const iconSaved = await uploadCustomIcon(fileInput.files[0]);

    if (!iconSaved) return;

    await new Promise(resolve => setTimeout(resolve, 300));

    await loadTiles();

    closeChangeIcon();
}


function closeChangeIcon() {
    document.getElementById("change-icon-modal")
        .classList.add("hidden");

    selectedSlot = null;
}


/*
 * =====================================================
 * Installed Applications
 * =====================================================
 */

async function openInstalledApps() {
    if (selectedSlot === null) return;

    try {
        const response = await fetch("/api/installed-apps");

        if (!response.ok) {
            throw new Error("Failed to load installed applications.");
        }

        installedApps = await response.json();
        selectedInstalledApp = null;

        document.getElementById("installed-app-slot").textContent =
            `Slot ${selectedSlot}`;

        document.getElementById("installed-app-search").value = "";

        document.getElementById("installed-app-selected").textContent =
            "Selected: None";

        resetIconPreview("installed-app-custom-icon");

        document.getElementById("installed-app-list")
            .classList.remove("hidden");

        displayInstalledApps(installedApps);

        document.getElementById("config-modal").classList.add("hidden");

        document.getElementById("installed-app-modal")
            .classList.remove("hidden");

    } catch (error) {
        console.error("Failed to load installed applications:", error);
        alert("Failed to load installed applications.");
    }
}


function displayInstalledApps(apps) {
    const list = document.getElementById("installed-app-list");

    list.innerHTML = "";

    apps.forEach(app => {
        const item = document.createElement("div");

        item.className = "installed-app-item";

        const name = document.createElement("div");

        name.className = "installed-app-item-name";
        name.textContent = app.name;

        item.appendChild(name);

        item.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            selectInstalledApp(app);
        });

        list.appendChild(item);
    });
}


function searchInstalledApps() {
    const searchText = document.getElementById("installed-app-search")
        .value.toLowerCase().trim();

    document.getElementById("installed-app-list")
        .classList.remove("hidden");

    const filteredApps = installedApps.filter(
        app => app.name.toLowerCase().includes(searchText)
    );

    displayInstalledApps(filteredApps);
}


function selectInstalledApp(app) {
    selectedInstalledApp = app;

    document.getElementById("installed-app-selected").textContent =
        `Selected: ${app.name}`;

    document.getElementById("installed-app-list")
        .classList.add("hidden");
}


async function saveInstalledApp() {
    if (selectedSlot === null) return;

    if (!selectedInstalledApp) {
        alert("Please select an application first.");
        return;
    }

    try {
        const response = await fetch("/api/installed-apps/select", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                slotNumber: selectedSlot,
                name: selectedInstalledApp.name,
                appId: selectedInstalledApp.appId
            })
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const iconSaved = await saveCustomIconIfSelected();

        if (!iconSaved) return;

        await new Promise(resolve => setTimeout(resolve, 500));

        await loadTiles();

        closeInstalledApps();

    } catch (error) {
        console.error("Failed to save installed application:", error);
        alert("Failed to save installed application.");
    }
}


function closeInstalledApps() {
    document.getElementById("installed-app-modal")
        .classList.add("hidden");

    selectedInstalledApp = null;
    selectedSlot = null;
}


/*
 * =====================================================
 * Custom Application
 * =====================================================
 */

function openCustomApp() {
    if (selectedSlot === null) return;

    document.getElementById("custom-app-slot").textContent =
        `Slot ${selectedSlot}`;

    document.getElementById("custom-app-path").value = "";

    document.getElementById("custom-app-name").textContent =
        "Application: -";

    resetIconPreview("custom-app-custom-icon");

    document.getElementById("config-modal").classList.add("hidden");

    document.getElementById("custom-app-modal")
        .classList.remove("hidden");
}


function updateCustomAppName() {
    const path = document.getElementById("custom-app-path")
        .value.trim();

    if (!path) {
        document.getElementById("custom-app-name").textContent =
            "Application: -";

        return;
    }

    const fileName = path.split("\\").pop().split("/").pop();

    const appName = fileName.replace(/\.exe$/i, "");

    document.getElementById("custom-app-name").textContent =
        `Application: ${appName}`;
}


async function saveCustomApp() {
    if (selectedSlot === null) return;

    const path = document.getElementById("custom-app-path")
        .value.trim();

    if (!path) {
        alert("Please enter an EXE path.");
        return;
    }

    const fileName = path.split("\\").pop().split("/").pop();

    const name = fileName.replace(/\.exe$/i, "");

    try {
        const response = await fetch("/api/custom-app/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                slotNumber: selectedSlot,
                name: name,
                path: path
            })
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const iconSaved = await saveCustomIconIfSelected();

        if (!iconSaved) return;

        await new Promise(resolve => setTimeout(resolve, 300));

        await loadTiles();

        closeCustomApp();

    } catch (error) {
        console.error("Failed to save custom application:", error);
        alert("Failed to save custom application.");
    }
}


function closeCustomApp() {
    document.getElementById("custom-app-modal")
        .classList.add("hidden");

    selectedSlot = null;
}


/*
 * =====================================================
 * Website
 * =====================================================
 */

function openWebsite() {
    if (selectedSlot === null) return;

    document.getElementById("website-slot").textContent =
        `Slot ${selectedSlot}`;

    document.getElementById("website-url").value = "";

    document.getElementById("website-name").textContent =
        "Website: -";

    resetIconPreview("website-custom-icon");

    document.getElementById("config-modal").classList.add("hidden");

    document.getElementById("website-modal")
        .classList.remove("hidden");
}


function updateWebsiteName() {
    const url = document.getElementById("website-url")
        .value.trim();

    if (!url) {
        document.getElementById("website-name").textContent =
            "Website: -";

        return;
    }

    let previewUrl = url;

    if (
        !previewUrl.startsWith("http://") &&
        !previewUrl.startsWith("https://")
    ) {
        previewUrl = "https://" + previewUrl;
    }

    try {
        const parsedUrl = new URL(previewUrl);

        document.getElementById("website-name").textContent =
            `Website: ${parsedUrl.hostname}`;

    } catch (error) {
        document.getElementById("website-name").textContent =
            "Website: Invalid URL";
    }
}


async function saveWebsite() {
    if (selectedSlot === null) return;

    let url = document.getElementById("website-url")
        .value.trim();

    if (!url) {
        alert("Please enter a website URL.");
        return;
    }

    if (
        !url.startsWith("http://") &&
        !url.startsWith("https://")
    ) {
        url = "https://" + url;
    }

    try {
        const parsedUrl = new URL(url);

        const name = parsedUrl.hostname;

        const response = await fetch("/api/website/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                slotNumber: selectedSlot,
                name: name,
                url: url
            })
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const iconSaved = await saveCustomIconIfSelected();

        if (!iconSaved) return;

        await loadTiles();

        closeWebsite();

    } catch (error) {
        console.error("Failed to save website:", error);
        alert("Failed to save website.");
    }
}


function closeWebsite() {
    document.getElementById("website-modal")
        .classList.add("hidden");

    selectedSlot = null;
}


/*
 * =====================================================
 * Keyboard Shortcuts
 * =====================================================
 */

function openKeyboardShortcut() {
    if (selectedSlot === null) return;

    const tile = currentTiles.find(
        item => item.slotNumber === selectedSlot
    );

    document.getElementById("keyboard-shortcut-slot")
        .textContent = `Slot ${selectedSlot}`;

    document.getElementById("shortcut-name").value =
        tile && tile.type === "KEYBOARD_SHORTCUT"
            ? tile.name || ""
            : "";

    const shortcutInput = document.getElementById("shortcut-input");

    shortcutInput.value =
        tile && tile.type === "KEYBOARD_SHORTCUT"
            ? tile.target || ""
            : "";

    /*
     * Allow manual typing even if the old HTML
     * still contains the readonly attribute.
     */
    shortcutInput.readOnly = false;
    shortcutInput.removeAttribute("readonly");

    shortcutInput.placeholder = "Example: CTRL+SHIFT+9";

    document.getElementById("keyboard-shortcut-error")
        .textContent = "";

    resetIconPreview("shortcut-icon-file");

    /*
     * Remove the old Record Shortcut button
     * if it is still present in index.html.
     */
    const recordButton = document.getElementById(
        "record-shortcut-button"
    );

    if (recordButton) {
        recordButton.remove();
    }

    document.getElementById("config-modal").classList.add("hidden");

    document.getElementById("keyboard-shortcut-modal")
        .classList.remove("hidden");
}


function normalizeKeyboardShortcut(value) {
    return value
        .toUpperCase()
        .replace(/\s+/g, "")
        .replace(/CONTROL/g, "CTRL")
        .replace(/ESCAPE/g, "ESC")
        .replace(/ARROWUP/g, "UP")
        .replace(/ARROWDOWN/g, "DOWN")
        .replace(/ARROWLEFT/g, "LEFT")
        .replace(/ARROWRIGHT/g, "RIGHT");
}


function validateKeyboardShortcut(shortcut) {
    if (!shortcut) {
        return "Please enter a keyboard shortcut.";
    }

    const normalized = normalizeKeyboardShortcut(shortcut);
    const parts = normalized.split("+");

    if (parts.length < 2 || parts.some(part => !part)) {
        return "Use a format like CTRL+SHIFT+9.";
    }

    const modifiers = ["CTRL", "ALT", "SHIFT"];
    const selectedModifiers = parts.slice(0, -1);
    const mainKey = parts[parts.length - 1];

    if (selectedModifiers.length > 3) {
        return "Too many modifier keys.";
    }

    if (selectedModifiers.some(modifier => !modifiers.includes(modifier))) {
        return "Use CTRL, ALT or SHIFT as modifiers.";
    }

    if (new Set(selectedModifiers).size !== selectedModifiers.length) {
        return "Duplicate modifier keys are not allowed.";
    }

    const validMainKey =
        /^[A-Z0-9]$/.test(mainKey) ||
        /^F([1-9]|1[0-2])$/.test(mainKey) ||
        [
            "SPACE", "ESC", "ENTER", "TAB",
            "BACKSPACE", "DELETE", "INSERT",
            "HOME", "END", "PAGEUP", "PAGEDOWN",
            "UP", "DOWN", "LEFT", "RIGHT",
            "CAPSLOCK", "PRINTSCREEN", "SCROLLLOCK",
            "PAUSE", "NUMPADENTER"
        ].includes(mainKey);

    if (!validMainKey) {
        return "Unsupported key. Please check the shortcut format.";
    }

    const blockedShortcuts = [
        "ALT+F4",
        "CTRL+SHIFT+ESC",
        "CTRL+ALT+DELETE",
        "ALT+TAB"
    ];

    if (blockedShortcuts.includes(normalized)) {
        return "This system shortcut is not allowed.";
    }

    return null;
}


async function saveKeyboardShortcut() {
    if (selectedSlot === null) return;

    const name = document.getElementById("shortcut-name")
        .value.trim();

    const shortcutInput = document.getElementById("shortcut-input");

    const shortcut = normalizeKeyboardShortcut(
        shortcutInput.value
    );

    const errorElement = document.getElementById(
        "keyboard-shortcut-error"
    );

    if (!name) {
        errorElement.textContent = "Please enter a shortcut name.";
        return;
    }

    const validationError = validateKeyboardShortcut(shortcut);

    if (validationError) {
        errorElement.textContent = validationError;
        return;
    }

    const tile = currentTiles.find(
        item => item.slotNumber === selectedSlot
    );

    if (!tile || tile.id == null) {
        errorElement.textContent =
            "Could not find the database record for this slot.";

        return;
    }

    try {
        errorElement.textContent = "Saving shortcut...";

        const response = await fetch(`/api/tiles/${tile.id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: name,
                type: "KEYBOARD_SHORTCUT",
                target: shortcut,
                icon: tile.icon || null
            })
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const iconFileInput = document.getElementById(
            "shortcut-icon-file"
        );

        if (iconFileInput.files.length > 0) {
            const iconSaved = await uploadCustomIcon(
                iconFileInput.files[0]
            );

            if (!iconSaved) {
                errorElement.textContent =
                    "Shortcut saved, but icon upload failed.";

                await loadTiles();
                return;
            }
        }

        await loadTiles();

        document.getElementById("keyboard-shortcut-modal")
            .classList.add("hidden");

        selectedSlot = null;

    } catch (error) {
        console.error("Failed to save keyboard shortcut:", error);

        errorElement.textContent =
            "Failed to save keyboard shortcut.";

        alert("Failed to save keyboard shortcut.");
    }
}


function closeKeyboardShortcut() {
    document.getElementById("keyboard-shortcut-modal")
        .classList.add("hidden");

    document.getElementById("config-modal")
        .classList.remove("hidden");

    document.getElementById("keyboard-shortcut-error")
        .textContent = "";
}


/*
 * =====================================================
 * Event Listeners
 * =====================================================
 */

/* Configure Tile */

document.getElementById("installed-app-button")
    .addEventListener("click", openInstalledApps);

document.getElementById("custom-app-button")
    .addEventListener("click", openCustomApp);

document.getElementById("website-button")
    .addEventListener("click", openWebsite);

document.getElementById("system-button-button")
    .addEventListener("click", openSystemButton);

document.getElementById("keyboard-shortcut-button")
    .addEventListener("click", openKeyboardShortcut);


/* System Button */

document.querySelectorAll(".system-button-option")
    .forEach(button => {
        button.addEventListener("click", () => {
            saveSystemButton(button.dataset.action);
        });
    });

document.getElementById("system-button-close-button")
    .addEventListener("click", closeSystemButton);


/* Change Icon */

document.getElementById("change-icon-button")
    .addEventListener("click", openChangeIcon);

document.getElementById("change-icon-save-button")
    .addEventListener("click", saveIndependentIcon);

document.getElementById("change-icon-close-button")
    .addEventListener("click", closeChangeIcon);


/* Delete Configuration */

document.getElementById("delete-configuration-button")
    .addEventListener("click", openDeleteConfiguration);

document.getElementById("config-cancel-button")
    .addEventListener("click", closeConfiguration);

document.getElementById("delete-configuration-yes-button")
    .addEventListener("click", deleteConfiguration);

document.getElementById("delete-configuration-no-button")
    .addEventListener("click", cancelDeleteConfiguration);


/* Installed Applications */

document.getElementById("installed-app-search")
    .addEventListener("input", searchInstalledApps);

document.getElementById("installed-app-save-button")
    .addEventListener("click", saveInstalledApp);

document.getElementById("installed-app-close-button")
    .addEventListener("click", closeInstalledApps);


/* Custom Application */

document.getElementById("custom-app-path")
    .addEventListener("input", updateCustomAppName);

document.getElementById("custom-app-save-button")
    .addEventListener("click", saveCustomApp);

document.getElementById("custom-app-close-button")
    .addEventListener("click", closeCustomApp);


/* Website */

document.getElementById("website-url")
    .addEventListener("input", updateWebsiteName);

document.getElementById("website-save-button")
    .addEventListener("click", saveWebsite);

document.getElementById("website-close-button")
    .addEventListener("click", closeWebsite);


/* Keyboard Shortcuts */

document.getElementById("keyboard-shortcut-save-button")
    .addEventListener("click", saveKeyboardShortcut);

document.getElementById("keyboard-shortcut-cancel-button")
    .addEventListener("click", closeKeyboardShortcut);


/*
 * =====================================================
 * Initialize Icon Previews
 * =====================================================
 */

setupIconPreview("installed-app-custom-icon");
setupIconPreview("custom-app-custom-icon");
setupIconPreview("website-custom-icon");
setupIconPreview("shortcut-icon-file");
setupIconPreview("change-icon-file");


/*
 * =====================================================
 * Initial Page Load
 * =====================================================
 */

loadServerInfo();
loadTiles();
