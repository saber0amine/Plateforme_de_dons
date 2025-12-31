document.addEventListener('DOMContentLoaded', function() {
    initializeAlerts();
    initializeTooltips();
    initializeConfirmDialogs();
    initializeTextareas();
    initializeCharacterCounters();
    initializeImageUpload();
    initializeScrollToTop();
    initializeSearchSuggestions();
    initializeFormValidation();
    initializeLazyLoading();
    initializeSkeletonLoaders();
});

function initializeAlerts() {
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) {
                bsAlert.close();
            }
        }, 8000);
    });
}

function initializeTooltips() {
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl, {
        trigger: 'hover',
        delay: { show: 500, hide: 100 }
    }));
}

function initializeConfirmDialogs() {
    const deleteButtons = document.querySelectorAll('[data-confirm]');
    deleteButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            const message = this.dataset.confirm || 'Êtes-vous sûr de vouloir continuer ?';
            if (!showConfirmDialog(message)) {
                e.preventDefault();
            }
        });
    });
}

function showConfirmDialog(message) {
    return confirm(message);
}

function showCustomConfirmDialog(title, message, confirmCallback) {
    const modalHtml = `
        <div class="modal fade" id="confirmModal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">${title}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p>${message}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                        <button type="button" class="btn btn-danger" id="confirmBtn">Confirmer</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modal = new bootstrap.Modal(document.getElementById('confirmModal'));

    document.getElementById('confirmBtn').addEventListener('click', function() {
        confirmCallback();
        modal.hide();
    });

    modal.show();

    document.getElementById('confirmModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

function initializeTextareas() {
    const textareas = document.querySelectorAll('textarea[data-autoresize]');
    textareas.forEach(function(textarea) {
        textarea.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = this.scrollHeight + 'px';
        });
        textarea.dispatchEvent(new Event('input'));
    });
}

function initializeCharacterCounters() {
    const fields = document.querySelectorAll('textarea[maxlength], input[maxlength]');
    fields.forEach(function(field) {
        const maxLength = field.getAttribute('maxlength');
        if (maxLength) {
            const counterContainer = document.createElement('div');
            counterContainer.className = 'd-flex justify-content-between align-items-center mt-1';

            const counter = document.createElement('small');
            counter.className = 'text-muted ms-auto';
            counter.setAttribute('aria-live', 'polite');
            updateCounter(field, counter, maxLength);

            counterContainer.appendChild(counter);
            field.parentNode.appendChild(counterContainer);

            field.addEventListener('input', function() {
                updateCounter(this, counter, maxLength);
            });
        }
    });
}

function updateCounter(field, counter, maxLength) {
    const currentLength = field.value.length;
    counter.textContent = `${currentLength}/${maxLength}`;

    if (currentLength > maxLength * 0.9) {
        counter.classList.add('text-warning');
    } else {
        counter.classList.remove('text-warning');
    }

    if (currentLength >= maxLength) {
        counter.classList.remove('text-warning');
        counter.classList.add('text-danger');
    } else {
        counter.classList.remove('text-danger');
    }
}

function initializeImageUpload() {
    const imageInputs = document.querySelectorAll('input[type="file"][accept*="image"]');

    imageInputs.forEach(function(input) {
        const parent = input.parentElement;
        parent.classList.add('image-upload-zone');

        parent.addEventListener('dragover', function(e) {
            e.preventDefault();
            this.classList.add('drag-over');
        });

        parent.addEventListener('dragleave', function(e) {
            e.preventDefault();
            this.classList.remove('drag-over');
        });

        parent.addEventListener('drop', function(e) {
            e.preventDefault();
            this.classList.remove('drag-over');

            const files = e.dataTransfer.files;
            if (files.length > 0) {
                input.files = files;
                input.dispatchEvent(new Event('change', { bubbles: true }));
            }
        });
    });
}

function showLoadingOverlay(message = 'Chargement...') {
    let overlay = document.getElementById('loadingOverlay');

    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'loadingOverlay';
        overlay.className = 'loading-overlay';
        overlay.innerHTML = `
            <div class="text-center">
                <div class="loading-spinner"></div>
                <p class="text-white mt-3">${message}</p>
            </div>
        `;
        document.body.appendChild(overlay);
    }

    setTimeout(() => {
        overlay.classList.add('active');
    }, 10);
}

function hideLoadingOverlay() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.classList.remove('active');
        setTimeout(() => {
            overlay.remove();
        }, 300);
    }
}

function initializeScrollToTop() {
    let scrollButton = document.getElementById('scrollToTop');

    if (!scrollButton) {
        scrollButton = document.createElement('button');
        scrollButton.id = 'scrollToTop';
        scrollButton.innerHTML = '<i class="bi bi-arrow-up"></i>';
        scrollButton.setAttribute('aria-label', 'Retour en haut');
        document.body.appendChild(scrollButton);
    }

    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            scrollButton.classList.add('visible');
        } else {
            scrollButton.classList.remove('visible');
        }
    });

    scrollButton.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

function initializeSearchSuggestions() {
    const searchInputs = document.querySelectorAll('input[name="query"], input[name="keywords"]');

    searchInputs.forEach(function(input) {
        let suggestionsContainer = null;
        let debounceTimer = null;

        input.addEventListener('input', function() {
            clearTimeout(debounceTimer);

            const query = this.value.trim();

            if (query.length < 2) {
                if (suggestionsContainer) {
                    suggestionsContainer.remove();
                    suggestionsContainer = null;
                }
                return;
            }

            debounceTimer = setTimeout(() => {
                fetchSearchSuggestions(query, input);
            }, 300);
        });

        input.addEventListener('blur', function() {
            setTimeout(() => {
                if (suggestionsContainer) {
                    suggestionsContainer.remove();
                    suggestionsContainer = null;
                }
            }, 200);
        });
    });
}

function fetchSearchSuggestions(query, inputElement) {
    fetch(`/api/keywords?query=${encodeURIComponent(query)}`)
        .then(response => response.json())
        .then(suggestions => {
            displaySearchSuggestions(suggestions, inputElement);
        })
        .catch(error => {
            console.error('Erreur lors de la récupération des suggestions:', error);
        });
}

function displaySearchSuggestions(suggestions, inputElement) {
    let container = document.getElementById('searchSuggestions');

    if (!container) {
        container = document.createElement('div');
        container.id = 'searchSuggestions';
        container.className = 'search-suggestions';
        inputElement.parentElement.style.position = 'relative';
        inputElement.parentElement.appendChild(container);
    }

    if (suggestions.length === 0) {
        container.remove();
        return;
    }

    container.innerHTML = '';

    suggestions.forEach(function(suggestion) {
        const item = document.createElement('div');
        item.className = 'search-suggestion-item';
        item.textContent = suggestion.name || suggestion;
        item.addEventListener('click', function() {
            inputElement.value = this.textContent;
            container.remove();
            inputElement.focus();
        });
        container.appendChild(item);
    });
}

function initializeFormValidation() {
    const forms = document.querySelectorAll('form[data-validate]');

    forms.forEach(function(form) {
        form.addEventListener('submit', function(e) {
            if (!validateForm(this)) {
                e.preventDefault();
                showNotification('Veuillez corriger les erreurs dans le formulaire', 'error');

                const firstError = this.querySelector('.is-invalid');
                if (firstError) {
                    firstError.focus();
                    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        });

        const inputs = form.querySelectorAll('input, textarea, select');
        inputs.forEach(function(input) {
            input.addEventListener('blur', function() {
                validateField(this);
            });

            input.addEventListener('input', function() {
                if (this.classList.contains('is-invalid')) {
                    validateField(this);
                }
            });
        });
    });
}

function validateForm(form) {
    let isValid = true;
    const inputs = form.querySelectorAll('input[required], textarea[required], select[required]');

    inputs.forEach(function(input) {
        if (!validateField(input)) {
            isValid = false;
        }
    });

    return isValid;
}

function validateField(field) {
    const value = field.value.trim();
    const type = field.type;
    const required = field.hasAttribute('required');
    let isValid = true;
    let errorMessage = '';

    if (required && !value) {
        isValid = false;
        errorMessage = 'Ce champ est obligatoire';
    } else if (type === 'email' && value && !isValidEmail(value)) {
        isValid = false;
        errorMessage = 'Veuillez entrer une adresse email valide';
    } else if (field.hasAttribute('minlength') && value.length < field.getAttribute('minlength')) {
        isValid = false;
        errorMessage = `Minimum ${field.getAttribute('minlength')} caractères requis`;
    } else if (field.hasAttribute('maxlength') && value.length > field.getAttribute('maxlength')) {
        isValid = false;
        errorMessage = `Maximum ${field.getAttribute('maxlength')} caractères autorisés`;
    }

    if (isValid) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
        const feedback = field.parentElement.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.style.display = 'none';
        }
    } else {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
        let feedback = field.parentElement.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            field.parentElement.appendChild(feedback);
        }
        feedback.textContent = errorMessage;
        feedback.style.display = 'block';
    }

    return isValid;
}

function isValidEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

function showNotification(message, type = 'info') {
    const alertClass = type === 'error' ? 'alert-danger' : type === 'success' ? 'alert-success' : 'alert-info';
    const iconClass = type === 'error' ? 'bi-exclamation-triangle' : type === 'success' ? 'bi-check-circle' : 'bi-info-circle';

    const notification = document.createElement('div');
    notification.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    notification.style.zIndex = '9999';
    notification.style.minWidth = '300px';
    notification.setAttribute('role', 'alert');
    notification.innerHTML = `
        <i class="bi ${iconClass}"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
        const bsAlert = bootstrap.Alert.getOrCreateInstance(notification);
        bsAlert.close();
    }, 5000);
}

function initializeLazyLoading() {
    const lazyImages = document.querySelectorAll('img[data-src]');

    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.removeAttribute('data-src');
                    img.classList.add('fade-in');
                    observer.unobserve(img);
                }
            });
        });

        lazyImages.forEach(img => imageObserver.observe(img));
    } else {
        lazyImages.forEach(img => {
            img.src = img.dataset.src;
            img.removeAttribute('data-src');
        });
    }
}

function initializeSkeletonLoaders() {
    const loadingElements = document.querySelectorAll('[data-loading]');

    loadingElements.forEach(element => {
        const skeleton = createSkeleton(element.dataset.loading);
        element.innerHTML = skeleton;
    });
}

function createSkeleton(type) {
    switch(type) {
        case 'card':
            return `
                <div class="skeleton skeleton-image"></div>
                <div class="skeleton skeleton-title"></div>
                <div class="skeleton skeleton-text"></div>
                <div class="skeleton skeleton-text"></div>
            `;
        case 'list':
            return `
                <div class="skeleton skeleton-text mb-3"></div>
                <div class="skeleton skeleton-text mb-3"></div>
                <div class="skeleton skeleton-text mb-3"></div>
            `;
        default:
            return '<div class="skeleton skeleton-text"></div>';
    }
}

function fetchAPI(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    };

    showLoadingOverlay();

    return fetch(url, { ...defaultOptions, ...options })
        .then(response => {
            hideLoadingOverlay();

            if (!response.ok) {
                throw new Error('Erreur réseau');
            }
            return response.json();
        })
        .catch(error => {
            hideLoadingOverlay();
            showNotification('Une erreur est survenue. Veuillez réessayer.', 'error');
            throw error;
        });
}

function toggleFavorite(annonceId, button) {
    const isFavorite = button.classList.contains('btn-danger');
    const method = isFavorite ? 'DELETE' : 'POST';
    const url = isFavorite ? `/favorites/${annonceId}` : `/favorites/add/${annonceId}`;

    button.disabled = true;
    const originalHtml = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';

    fetchAPI(url, { method })
        .then(data => {
            if (data.success) {
                button.classList.toggle('btn-danger');
                button.classList.toggle('btn-outline-danger');
                const icon = button.querySelector('i') || document.createElement('i');
                icon.classList.toggle('bi-heart');
                icon.classList.toggle('bi-heart-fill');
                button.innerHTML = '';
                button.appendChild(icon);
                button.appendChild(document.createTextNode(isFavorite ? ' Ajouter aux favoris' : ' Retirer des favoris'));

                showNotification(
                    isFavorite ? 'Retiré des favoris' : 'Ajouté aux favoris',
                    'success'
                );
            }
        })
        .catch(error => {
            console.error('Erreur:', error);
            button.innerHTML = originalHtml;
        })
        .finally(() => {
            button.disabled = false;
        });
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

window.addEventListener('online', () => {
    showNotification('Connexion rétablie', 'success');
});

window.addEventListener('offline', () => {
    showNotification('Pas de connexion internet', 'error');
});

if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .then(registration => console.log('SW registered'))
            .catch(err => console.log('SW registration failed'));
    });
}