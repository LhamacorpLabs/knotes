async function loadComponent(componentPath, targetId, customizations = {}) {
    try {
        const response = await fetch(componentPath);
        if (!response.ok) throw new Error(`Failed to load ${componentPath}`);

        let html = await response.text();

        Object.entries(customizations).forEach(([placeholder, value]) => {
            html = html.replace(new RegExp(`{{${placeholder}}}`, 'g'), value);
        });

        const targetElement = document.getElementById(targetId);
        if (targetElement) {
            targetElement.innerHTML = html;
        }
    } catch (error) {
        console.error('Component loading failed:', error);
    }
}

const PAGE_CONFIGS = {
    editor: {
        header: `
            <span class="note-id" id="noteIdDisplay" style="display: none; cursor: pointer;" onclick="copyNoteLink()" title="Click to copy note link"></span>
            <div class="theme-switch" id="themeSwitch" onclick="toggleTheme()" title="Toggle dark/light theme"></div>
            <button class="new-btn" onclick="showIdInput()">Open</button>
            <button class="new-btn" onclick="newNote()">New</button>
        `,
        needsModal: true,
        needsToast: true
    },
    home: {
        header: `
            <div class="theme-switch" id="themeSwitch" onclick="toggleTheme()" title="Toggle dark/light theme"></div>
        `,
        needsModal: true,
        needsToast: false
    },
    error: {
        header: `
            <div class="theme-switch" id="themeSwitch" onclick="toggleTheme()" title="Toggle dark/light theme"></div>
            <button class="new-btn" onclick="window.location.href='/'">Home</button>
        `,
        needsModal: true,
        needsToast: false
    }
};

async function initPage(pageType) {
    const config = PAGE_CONFIGS[pageType];
    if (!config) {
        console.error('Unknown page type:', pageType);
        return;
    }

    await loadComponent('/components/header.html', 'headerContainer', {
        HEADER_CONTENT: config.header
    });

    if (config.needsModal) {
        await loadComponent('/components/modal.html', 'modalContainer');
    }

    if (typeof initializeTheme === 'function') {
        initializeTheme();
    }

    updateThemeSwitchState();
}