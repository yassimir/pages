(function () {

    const modal = document.getElementById('paymentDetailsModal');
    if (!modal) return;

    const loading = document.getElementById('paymentDetailLoading');
    const content = document.getElementById('paymentDetailContent');
    const error = document.getElementById('paymentDetailError');

    let currentPaymentId = null;
    let detailController = null;

    function showLoading() {
        loading.style.display = 'block';
        content.style.display = 'none';
        error.style.display = 'none';
    }

    function showError() {
        loading.style.display = 'none';
        content.style.display = 'none';
        error.style.display = 'block';
    }

    function showContent() {
        loading.style.display = 'none';
        error.style.display = 'none';
        content.style.display = 'block';
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (!element) return;

        element.textContent =
            value !== null && value !== undefined && value !== ''
                ? value
                : '-';
    }

    function loadPaymentDetails(id) {

        currentPaymentId = id;

        if (detailController) {
            detailController.abort();
        }

        detailController = new AbortController();

        showLoading();

        fetch('/payments/' + encodeURIComponent(id) + '/details', {
            method: 'GET',
            signal: detailController.signal
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Impossible de récupérer le paiement');
                }
                return response.json();
            })
            .then(payment => {

                setText('detailReference', payment.id);
                setText('detailClient', payment.client);
                setText('detailPhone', payment.telephone);
                setText('detailDate', payment.date);

                setText(
                    'detailAmount',
                    payment.montant !== null
                        ? payment.montant + ' DH'
                        : '0,00 DH'
                );

                setText('detailMethod', payment.methode);

                const accountName = payment.compteNom || '-';
                const accountType = payment.compteType || '';

                setText(
                    'detailAccount',
                    accountType
                        ? accountName + ' (' + accountType + ')'
                        : accountName
                );

                const receiptContainer =
                    document.getElementById('detailReceiptContainer');

                const receiptEmpty =
                    document.getElementById('detailReceiptEmpty');

                const receiptImage =
                    document.getElementById('detailReceiptImage');

                const receiptLink =
                    document.getElementById('detailReceiptLink');

                const receiptFile =
                    document.getElementById('detailReceiptFile');

                if (payment.justificatifUrl) {

                    receiptContainer.style.display = 'block';
                    receiptEmpty.style.display = 'none';

                    const mime = payment.justificatifTypeMime || '';

                    receiptLink.href = payment.justificatifUrl;

                    if (mime.startsWith('image/')) {

                        receiptImage.src = payment.justificatifUrl;
                        receiptImage.style.display = 'block';
                        receiptFile.style.display = 'none';

                    } else {

                        receiptImage.style.display = 'none';
                        receiptImage.removeAttribute('src');

                        receiptFile.href = payment.justificatifUrl;
                        receiptFile.style.display = 'inline-block';
                    }

                } else {

                    receiptContainer.style.display = 'none';
                    receiptEmpty.style.display = 'block';

                    receiptImage.removeAttribute('src');
                    receiptImage.style.display = 'none';
                    receiptFile.style.display = 'none';
                }

                const status = document.getElementById('detailStatus');

                status.textContent = payment.confirmed
                    ? 'Confirmé'
                    : 'En attente';

                status.className = payment.confirmed
                    ? 'detail-value status confirmed'
                    : 'detail-value status pending';

                showContent();
            })
            .catch(err => {
                if (err.name !== 'AbortError') {
                    console.error(err);
                    showError();
                }
            });
    }

    document.addEventListener('click', function (event) {

        const button = event.target.closest(
            '[data-target="#paymentDetailsModal"]'
        );

        if (!button) return;

        const id = button.getAttribute('data-payment-id');

        if (id) {
            loadPaymentDetails(id);
        }
    });

    const retryButton = document.getElementById('retryPaymentDetails');

    if (retryButton) {
        retryButton.addEventListener('click', function () {

            if (currentPaymentId !== null) {
                loadPaymentDetails(currentPaymentId);
            }
        });
    }

    if (window.jQuery) {
        window.jQuery(modal).on('hidden.bs.modal', function () {

            if (detailController) {
                detailController.abort();
                detailController = null;
            }
        });
    }

})();