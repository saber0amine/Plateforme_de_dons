// Plateforme de Dons - JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        }, 5000);
    });

    // Enable tooltips
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function(tooltipTriggerEl) {
        new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Confirm delete actions
    const deleteButtons = document.querySelectorAll('[data-confirm]');
    deleteButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            if (!confirm(this.dataset.confirm)) {
                e.preventDefault();
            }
        });
    });

    // Auto-resize textareas
    const textareas = document.querySelectorAll('textarea[data-autoresize]');
    textareas.forEach(function(textarea) {
        textarea.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = this.scrollHeight + 'px';
        });
    });

    // Character counter for description fields
    const descriptionFields = document.querySelectorAll('textarea[maxlength]');
    descriptionFields.forEach(function(field) {
        const maxLength = field.getAttribute('maxlength');
        if (maxLength) {
            const counter = document.createElement('small');
            counter.className = 'text-muted float-end';
            counter.textContent = `0/${maxLength}`;
            field.parentNode.appendChild(counter);

            field.addEventListener('input', function() {
                counter.textContent = `${this.value.length}/${maxLength}`;
                if (this.value.length > maxLength * 0.9) {
                    counter.classList.add('text-danger');
                } else {
                    counter.classList.remove('text-danger');
                }
            });
        }
    });
});

// Utility function for AJAX requests
function fetchAPI(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    };
    return fetch(url, { ...defaultOptions, ...options })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        });
}

// Toggle favorite (for AJAX implementation)
function toggleFavorite(annonceId, button) {
    const isFavorite = button.classList.contains('btn-danger');
    const method = isFavorite ? 'DELETE' : 'POST';
    const url = isFavorite ? `/favorites/${annonceId}` : `/favorites/add/${annonceId}`;

    fetchAPI(url, { method })
        .then(data => {
            if (data.success) {
                button.classList.toggle('btn-danger');
                button.classList.toggle('btn-outline-danger');
                const icon = button.querySelector('i');
                icon.classList.toggle('bi-heart');
                icon.classList.toggle('bi-heart-fill');
            }
        })
        .catch(error => console.error('Error:', error));
}
