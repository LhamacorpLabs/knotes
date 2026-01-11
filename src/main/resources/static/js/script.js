const API_BASE = '/api/notes';
let currentNoteId = null;
let autoSaveTimeout = null;
let lastSavedContent = '';
let currentNoteModifiedAt = null;
let versionCheckInterval = null;
const VERSION_CHECK_INTERVAL_MS = 5000;

// Theme management
function initializeTheme() {
    // Get saved theme preference or default to dark
    const savedTheme = localStorage.getItem('theme') || 'dark';
    const body = document.body;

    // Apply theme to body first (this works even if switch isn't loaded yet)
    if (savedTheme === 'light') {
        body.setAttribute('data-theme', 'light');
    } else {
        body.removeAttribute('data-theme');
    }

    // Update theme switch if it exists
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

    // Toggle between light and dark themes
    const currentTheme = body.getAttribute('data-theme');

    if (currentTheme === 'light') {
        // Switch to dark theme
        body.removeAttribute('data-theme');
        themeSwitch.classList.add('dark');
        localStorage.setItem('theme', 'dark');
    } else {
        // Switch to light theme
        body.setAttribute('data-theme', 'light');
        themeSwitch.classList.remove('dark');
        localStorage.setItem('theme', 'light');
    }

    // Update theme color for mobile browsers
    updateThemeColor();
}

function init() {
    // Initialize theme first
    initializeTheme();

    // Mobile-specific optimizations
    setupMobileOptimizations();

    // Extract note ID from URL path (e.g., /01KDECFWYDMS857DZMCR680MCY)
    const pathname = window.location.pathname;
    const pathParts = pathname.split('/');
    let idFromUrl = null;

    // Look for a 26-character alphanumeric string (ULID pattern)
    for (const part of pathParts) {
        if (part && /^[A-Za-z0-9]{26}$/.test(part)) {
            idFromUrl = part;
            break;
        }
    }

    // Only load/create notes if we're on the note editor page (has noteContent element)
    const noteContent = document.getElementById('noteContent');
    if (noteContent) {
        if (idFromUrl) {
            loadNoteById(idFromUrl);
        } else {
            newNote();
        }
    }

    // Set up auto-save on content change
    if (noteContent) {
        noteContent.addEventListener('input', handleContentChange);
    }

    // Handle Enter key in ID input
    const noteIdInput = document.getElementById('noteIdInput');
    if (noteIdInput) {
        noteIdInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                loadNoteFromInput();
            }
        });
    }
}

// Mobile-specific optimizations
function setupMobileOptimizations() {
    // Prevent iOS zoom on input focus by ensuring font-size is at least 16px
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

    if (isMobile) {
        // Handle virtual keyboard on mobile
        const noteContent = document.getElementById('noteContent');
        if (noteContent) {
            noteContent.addEventListener('focus', function() {
                // Small delay to let the virtual keyboard appear
                setTimeout(() => {
                    this.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }, 300);
            });

            // Prevent losing focus when scrolling on mobile
            noteContent.addEventListener('touchmove', function(e) {
                e.stopPropagation();
            }, { passive: true });
        }

        // Update theme color based on current theme
        updateThemeColor();
    }

    // Handle orientation change
    window.addEventListener('orientationchange', function() {
        // Fix viewport height issues on mobile browsers
        setTimeout(() => {
            const vh = window.innerHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);
        }, 100);
    });

    // Set initial viewport height
    const vh = window.innerHeight * 0.01;
    document.documentElement.style.setProperty('--vh', `${vh}px`);
}

// Update theme color meta tag based on current theme
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

// Handle content change for auto-save
function handleContentChange() {
    const noteContent = document.getElementById('noteContent');
    if (!noteContent) {
        console.error('Element with ID "noteContent" not found in handleContentChange');
        return;
    }
    const content = noteContent.value;

    // Clear existing timeout
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
            // Update existing note
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
                // Update version tracking after successful save
                setCurrentNoteVersion(updatedNote.modifiedAt);
            }
        } else {
            // Create new note
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

                // Update URL with new note ID (path-based)
                const newUrl = `/${note.id}`;
                window.history.replaceState({}, '', newUrl);

                // Show note ID in header
                showNoteId(note.id);

                // Set version tracking for new note
                setCurrentNoteVersion(note.modifiedAt);
            }
        }

        lastSavedContent = content;
    } catch (error) {
        console.error('Auto-save failed:', error);
    }
}

async function loadNoteById(id) {
    // Stop any existing version polling
    stopVersionPolling();

    try {
        const response = await fetch(`${API_BASE}/${id}`);

        if (response.ok) {
            const note = await response.json();
            currentNoteId = note.id;

            // Update content
            const noteContent = document.getElementById('noteContent');
            if (noteContent) {
                noteContent.value = note.content;
                lastSavedContent = note.content;
            } else {
                console.error('Element with ID "noteContent" not found in loadNoteById');
            }

            // Update URL (path-based)
            const newUrl = `/${note.id}`;
            window.history.replaceState({}, '', newUrl);

            // Show note ID in header
            showNoteId(note.id);

            // Set current version and start polling for updates
            setCurrentNoteVersion(note.modifiedAt);
        } else {
            // Note not found (404) or other error - create a new note instead
            console.warn(`Note with ID ${id} not found (${response.status}), creating new note`);
            await newNote();
        }
    } catch (error) {
        // Network error or other failure - create a new note instead
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
        console.error('Element with ID "noteIdDisplay" not found in showNoteId');
    }
}

async function copyNoteLink() {
    try {
        const noteIdDisplay = document.getElementById('noteIdDisplay');
        if (!noteIdDisplay) {
            console.error('Element with ID "noteIdDisplay" not found');
            return;
        }

        // Get the current URL
        const currentUrl = window.location.href;

        // Copy to clipboard
        await navigator.clipboard.writeText(currentUrl);

        // Visual feedback - temporarily show "Copied!" text
        const originalText = noteIdDisplay.textContent;
        noteIdDisplay.textContent = 'Copied!';
        noteIdDisplay.style.color = 'var(--accent-primary)';

        // Restore original text after 2 seconds
        setTimeout(() => {
            noteIdDisplay.textContent = originalText;
            noteIdDisplay.style.color = '';
        }, 2000);

    } catch (error) {
        console.error('Failed to copy note link:', error);

        // Fallback for older browsers - try to select the URL
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
    // Stop any existing version polling
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

            // Clear content and focus on content area
            const noteContent = document.getElementById('noteContent');
            if (noteContent) {
                noteContent.value = '';
                noteContent.focus();
            } else {
                console.error('Element with ID "noteContent" not found in newNote');
            }

            // Update URL with new note ID (path-based)
            const newUrl = `/${note.id}`;
            window.history.replaceState({}, '', newUrl);

            // Show note ID in header
            showNoteId(note.id);

            // Set current version and start polling for updates
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
    } else {
        console.error('Element with ID "idInputOverlay" not found in showIdInput');
    }

    if (noteIdInput) {
        noteIdInput.focus();
    } else {
        console.error('Element with ID "noteIdInput" not found in showIdInput');
    }
}

function hideIdInput() {
    const idInputOverlay = document.getElementById('idInputOverlay');
    const noteIdInput = document.getElementById('noteIdInput');

    if (idInputOverlay) {
        idInputOverlay.classList.add('hidden');
    } else {
        console.error('Element with ID "idInputOverlay" not found in hideIdInput');
    }

    if (noteIdInput) {
        noteIdInput.value = '';
    } else {
        console.error('Element with ID "noteIdInput" not found in hideIdInput');
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

// Version checking and polling
async function checkForNoteUpdates() {
    if (!currentNoteId || !currentNoteModifiedAt) {
        return; // No active note to check
    }

    try {
        const response = await fetch(`${API_BASE}/${currentNoteId}/metadata`);

        if (response.ok) {
            const metadata = await response.json();
            const serverModifiedAt = new Date(metadata.modifiedAt).getTime();
            const localModifiedAt = new Date(currentNoteModifiedAt).getTime();

            if (serverModifiedAt > localModifiedAt) {
                // Note has been updated elsewhere - auto-reload it
                await autoReloadNote();
            }
        }
    } catch (error) {
        console.error('Failed to check for note updates:', error);
    }
}

function startVersionPolling() {
    // Clear any existing polling
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

        // Hide after 2 seconds
        setTimeout(() => {
            toast.classList.add('hidden');
        }, 2000);
    }
}

async function autoReloadNote() {
    if (currentNoteId) {
        // Temporarily stop polling to avoid conflicts during reload
        stopVersionPolling();

        // Reload the note
        await loadNoteById(currentNoteId);

        // Show brief update message
        showUpdateToast();
    }
}

window.addEventListener('load', init);