const API_BASE = '/api/notes';
let currentNoteId = null;
let autoSaveTimeout = null;
let lastSavedContent = '';

// Initialize on page load
function init() {
    const urlParams = new URLSearchParams(window.location.search);
    const idFromUrl = urlParams.get('id');

    if (idFromUrl) {
        loadNoteById(idFromUrl);
    } else {
        // Create a new note immediately if no ID in URL
        newNote();
    }

    // Set up auto-save on content change
    const noteContent = document.getElementById('noteContent');
    noteContent.addEventListener('input', handleContentChange);

    // Handle Enter key in ID input
    document.getElementById('noteIdInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            loadNoteFromInput();
        }
    });
}

// Handle content change for auto-save
function handleContentChange() {
    const content = document.getElementById('noteContent').value;

    // Clear existing timeout
    if (autoSaveTimeout) {
        clearTimeout(autoSaveTimeout);
    }

    // Use 3-second debounce for all saves
    autoSaveTimeout = setTimeout(() => {
        autoSave(content);
    }, 3000);
}

// Auto-save function
async function autoSave(content) {
    // Don't save if content hasn't changed
    if (content === lastSavedContent) {
        return;
    }

    try {
        if (currentNoteId) {
            // Update existing note
            await fetch(API_BASE, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    id: currentNoteId,
                    content: content
                })
            });
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

                // Update URL with new note ID
                const url = new URL(window.location);
                url.searchParams.set('id', note.id);
                window.history.replaceState({}, '', url);

                // Show note ID in header
                showNoteId(note.id);
            }
        }

        lastSavedContent = content;
    } catch (error) {
        // Silently fail - no user feedback needed as per requirements
        console.error('Auto-save failed:', error);
    }
}

// Load note by ID
async function loadNoteById(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}`);

        if (response.ok) {
            const note = await response.json();
            currentNoteId = note.id;

            // Update content
            document.getElementById('noteContent').value = note.content;
            lastSavedContent = note.content;

            // Update URL
            const url = new URL(window.location);
            url.searchParams.set('id', note.id);
            window.history.replaceState({}, '', url);

            // Show note ID in header
            showNoteId(note.id);
        }
    } catch (error) {
        // Silently fail - no user feedback
        console.error('Failed to load note:', error);
    }
}

// Show note ID in header
function showNoteId(id) {
    const noteIdDisplay = document.getElementById('noteIdDisplay');
    noteIdDisplay.textContent = id;
    noteIdDisplay.style.display = 'inline-block';
}

// Hide note ID in header
function hideNoteId() {
    document.getElementById('noteIdDisplay').style.display = 'none';
}

// Create new note
async function newNote() {
    // Create empty note immediately to get an ID
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

            // Clear content
            document.getElementById('noteContent').value = '';

            // Update URL with new note ID
            const url = new URL(window.location);
            url.searchParams.set('id', note.id);
            window.history.replaceState({}, '', url);

            // Show note ID in header
            showNoteId(note.id);

            // Focus on content area
            document.getElementById('noteContent').focus();
        }
    } catch (error) {
        console.error('Failed to create new note:', error);
    }
}

// Show ID input overlay
function showIdInput() {
    document.getElementById('idInputOverlay').classList.remove('hidden');
    document.getElementById('noteIdInput').focus();
}

// Hide ID input overlay
function hideIdInput() {
    document.getElementById('idInputOverlay').classList.add('hidden');
    document.getElementById('noteIdInput').value = '';
}

// Load note from input
function loadNoteFromInput() {
    const id = document.getElementById('noteIdInput').value.trim();
    if (id) {
        hideIdInput();
        loadNoteById(id);
    }
}

// Initialize when page loads
window.addEventListener('load', init);