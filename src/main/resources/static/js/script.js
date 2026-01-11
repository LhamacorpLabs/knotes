const API_BASE = '/api/notes';
let currentNoteId = null;
let autoSaveTimeout = null;
let lastSavedContent = '';
let currentNoteModifiedAt = null;
let versionCheckInterval = null;
const VERSION_CHECK_INTERVAL_MS = 5000;

function initializeTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    const body = document.body;

    if (savedTheme === 'light') {
        body.setAttribute('data-theme', 'light');
    } else {
        body.removeAttribute('data-theme');
    }

    const themeSwitch = document.getElementById('themeSwitch');
    if (themeSwitch) {
        if (savedTheme === 'light') {
            themeSwitch.classList.remove('dark');
        } else {
            themeSwitch.classList.add('dark');
        }
    }

    updateThemeColor();
}

function updateThemeSwitchState() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    const themeSwitch = document.getElementById('themeSwitch');

    if (themeSwitch) {
        if (savedTheme === 'light') {
            themeSwitch.classList.remove('dark');
        } else {
            themeSwitch.classList.add('dark');
        }
    }
}

function toggleTheme() {
    const body = document.body;
    const themeSwitch = document.getElementById('themeSwitch');

    if (!themeSwitch) return;

    const currentTheme = body.getAttribute('data-theme');

    if (currentTheme === 'light') {
        body.removeAttribute('data-theme');
        themeSwitch.classList.add('dark');
        localStorage.setItem('theme', 'dark');
    } else {
        body.setAttribute('data-theme', 'light');
        themeSwitch.classList.remove('dark');
        localStorage.setItem('theme', 'light');
    }

    updateThemeColor();
}

function init() {
    initializeTheme();
    setupMobileOptimizations();

    const pathname = window.location.pathname;
    const pathParts = pathname.split('/');
    let idFromUrl = null;

    for (const part of pathParts) {
        if (part && /^[A-Za-z0-9]{26}$/.test(part)) {
            idFromUrl = part;
            break;
        }
    }

    const noteContent = document.getElementById('noteContent');
    if (noteContent) {
        if (idFromUrl) {
            loadNoteById(idFromUrl);
        } else {
            newNote();
        }
        noteContent.addEventListener('input', handleContentChange);
    }

    const noteIdInput = document.getElementById('noteIdInput');
    if (noteIdInput) {
        noteIdInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                loadNoteFromInput();
            }
        });
    }
}

function setupMobileOptimizations() {
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

    if (isMobile) {
        const noteContent = document.getElementById('noteContent');
        if (noteContent) {
            noteContent.addEventListener('focus', function() {
                setTimeout(() => {
                    this.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }, 300);
            });

            noteContent.addEventListener('touchmove', function(e) {
                e.stopPropagation();
            }, { passive: true });
        }

        updateThemeColor();
    }

    window.addEventListener('orientationchange', function() {
        setTimeout(() => {
            const vh = window.innerHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);
        }, 100);
    });

    const vh = window.innerHeight * 0.01;
    document.documentElement.style.setProperty('--vh', `${vh}px`);
}

function updateThemeColor() {
    const themeColorMeta = document.querySelector('meta[name="theme-color"]');
    const currentTheme = document.body.getAttribute('data-theme');

    if (themeColorMeta) {
        if (currentTheme === 'light') {
            themeColorMeta.setAttribute('content', '#007bff');
        } else {
            themeColorMeta.setAttribute('content', '#2d2d2d');
        }
    }
}

function handleContentChange() {
    const noteContent = document.getElementById('noteContent');
    if (!noteContent) {
        console.error('Element with ID "noteContent" not found');
        return;
    }
    const content = noteContent.value;

    if (autoSaveTimeout) {
        clearTimeout(autoSaveTimeout);
    }

    autoSaveTimeout = setTimeout(() => {
        autoSave(content);
    }, 1000);
}

async function autoSave(content) {
    if (content === lastSavedContent) {
        return;
    }

    try {
        if (currentNoteId) {
            const response = await fetch(`${API_BASE}/${currentNoteId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    content: content
                })
            });

            if (response.ok) {
                const updatedNote = await response.json();
                setCurrentNoteVersion(updatedNote.modifiedAt);
            }
        } else {
            const response = await fetch(API_BASE, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    note: content
                })
            });

            if (response.ok) {
                const note = await response.json();
                currentNoteId = note.id;

                const newUrl = `/${note.id}`;
                window.history.replaceState({}, '', newUrl);

                showNoteId(note.id);
                setCurrentNoteVersion(note.modifiedAt);
            }
        }

        lastSavedContent = content;
    } catch (error) {
        console.error('Auto-save failed:', error);
    }
}

async function loadNoteById(id) {
    stopVersionPolling();

    try {
        const response = await fetch(`${API_BASE}/${id}`);

        if (response.ok) {
            const note = await response.json();
            currentNoteId = note.id;

            const noteContent = document.getElementById('noteContent');
            if (noteContent) {
                noteContent.value = note.content;
                lastSavedContent = note.content;
            } else {
                console.error('Element with ID "noteContent" not found');
            }

            const newUrl = `/${note.id}`;
            window.history.replaceState({}, '', newUrl);

            showNoteId(note.id);
            setCurrentNoteVersion(note.modifiedAt);
        } else {
            console.warn(`Note with ID ${id} not found (${response.status}), creating new note`);
            await newNote();
        }
    } catch (error) {
        console.error('Failed to load note:', error, 'creating new note instead');
        await newNote();
    }
}

function showNoteId(id) {
    const noteIdDisplay = document.getElementById('noteIdDisplay');
    if (noteIdDisplay) {
        noteIdDisplay.textContent = id;
        noteIdDisplay.style.display = 'inline-block';
    } else {
        console.error('Element with ID "noteIdDisplay" not found');
    }
}

async function copyNoteLink() {
    try {
        const noteIdDisplay = document.getElementById('noteIdDisplay');
        if (!noteIdDisplay) {
            console.error('Element with ID "noteIdDisplay" not found');
            return;
        }

        const currentUrl = window.location.href;
        await navigator.clipboard.writeText(currentUrl);

        const originalText = noteIdDisplay.textContent;
        noteIdDisplay.textContent = 'Copied!';
        noteIdDisplay.style.color = 'var(--accent-primary)';

        setTimeout(() => {
            noteIdDisplay.textContent = originalText;
            noteIdDisplay.style.color = '';
        }, 2000);

    } catch (error) {
        console.error('Failed to copy note link:', error);

        const noteIdDisplay = document.getElementById('noteIdDisplay');
        if (noteIdDisplay) {
            const originalText = noteIdDisplay.textContent;
            noteIdDisplay.textContent = 'Copy failed';
            setTimeout(() => {
                noteIdDisplay.textContent = originalText;
            }, 2000);
        }
    }
}

async function newNote() {
    stopVersionPolling();

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                note: ''
            })
        });

        if (response.ok) {
            const note = await response.json();
            currentNoteId = note.id;
            lastSavedContent = '';

            const noteContent = document.getElementById('noteContent');
            if (noteContent) {
                noteContent.value = '';
                noteContent.focus();
            } else {
                console.error('Element with ID "noteContent" not found');
            }

            const newUrl = `/${note.id}`;
            window.history.replaceState({}, '', newUrl);

            showNoteId(note.id);
            setCurrentNoteVersion(note.modifiedAt);
        }
    } catch (error) {
        console.error('Failed to create new note:', error);
    }
}

function showIdInput() {
    const idInputOverlay = document.getElementById('idInputOverlay');
    const noteIdInput = document.getElementById('noteIdInput');

    if (idInputOverlay) {
        idInputOverlay.classList.remove('hidden');
    }

    if (noteIdInput) {
        noteIdInput.focus();
    }
}

function hideIdInput() {
    const idInputOverlay = document.getElementById('idInputOverlay');
    const noteIdInput = document.getElementById('noteIdInput');

    if (idInputOverlay) {
        idInputOverlay.classList.add('hidden');
    }

    if (noteIdInput) {
        noteIdInput.value = '';
    }
}

function loadNoteFromInput() {
    const noteIdInput = document.getElementById('noteIdInput');
    if (!noteIdInput) return;

    const id = noteIdInput.value.trim();
    if (id) {
        hideIdInput();
        window.location.href = `/${id}`;
    }
}

async function checkForNoteUpdates() {
    if (!currentNoteId || !currentNoteModifiedAt) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/${currentNoteId}/metadata`);

        if (response.ok) {
            const metadata = await response.json();
            const serverModifiedAt = new Date(metadata.modifiedAt).getTime();
            const localModifiedAt = new Date(currentNoteModifiedAt).getTime();

            if (serverModifiedAt > localModifiedAt) {
                await autoReloadNote();
            }
        }
    } catch (error) {
        console.error('Failed to check for note updates:', error);
    }
}

function startVersionPolling() {
    stopVersionPolling();

    if (currentNoteId) {
        versionCheckInterval = setInterval(checkForNoteUpdates, VERSION_CHECK_INTERVAL_MS);
    }
}

function stopVersionPolling() {
    if (versionCheckInterval) {
        clearInterval(versionCheckInterval);
        versionCheckInterval = null;
    }
}

function setCurrentNoteVersion(modifiedAt) {
    currentNoteModifiedAt = modifiedAt;
    startVersionPolling();
}

function showUpdateToast() {
    const toast = document.getElementById('updateToast');
    if (toast) {
        toast.classList.remove('hidden');

        setTimeout(() => {
            toast.classList.add('hidden');
        }, 2000);
    }
}

async function autoReloadNote() {
    if (currentNoteId) {
        stopVersionPolling();
        await loadNoteById(currentNoteId);
        showUpdateToast();
    }
}

window.addEventListener('load', init);