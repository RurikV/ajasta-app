import { useState, useCallback, useMemo } from 'react';

/**
 * Error modal that stays visible until user closes it.
 * Adds a Copy button to allow copying the error message to clipboard.
 * @param {string} message - The error message to display
 * @param {function} onDismiss - Callback to dismiss the error
 */
const ErrorDisplay = ({ message, onDismiss }) => {
    const [copied, setCopied] = useState(false);

    if (!message) return null;

    const overlayStyle = {
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        backgroundColor: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000
    };

    const modalStyle = {
        backgroundColor: '#fff',
        padding: '20px',
        borderRadius: '8px',
        maxWidth: '520px',
        width: '90%',
        boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
    };

    const headerStyle = { fontSize: '18px', fontWeight: '600', marginBottom: '10px', color: '#b00020' };
    const bodyStyle = { marginBottom: '16px', color: '#333', whiteSpace: 'pre-wrap' };
    const buttonRow = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' };
    const leftGroup = { display: 'flex', alignItems: 'center', gap: '8px' };
    const rightGroup = { display: 'flex', alignItems: 'center', gap: '8px', marginLeft: 'auto' };

    const copyBtn = {
        padding: '8px 16px',
        backgroundColor: '#6c757d',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer'
    };
    const closeBtn = {
        padding: '8px 16px',
        backgroundColor: '#007bff',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer'
    };

    const stop = (e) => e.stopPropagation();

    const toPlainString = (val) => {
        try {
            if (val == null) return '';
            if (typeof val === 'string') return val;
            if (val instanceof Error) return val.message || String(val);
            return JSON.stringify(val, null, 2);
        } catch {
            return String(val);
        }
    };

    const handleCopy = async () => {
        const text = toPlainString(message);
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
            } else {
                const ta = document.createElement('textarea');
                ta.value = text;
                ta.style.position = 'fixed';
                ta.style.top = '-1000px';
                document.body.appendChild(ta);
                ta.focus();
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
            }
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch (e) {
            // If copy fails, still allow user to select text manually
            console.error('Copy failed:', e);
        }
    };

    return (
        <div className="error-modal-overlay" style={overlayStyle} onClick={onDismiss}>
            <div className="error-modal" style={modalStyle} role="dialog" aria-modal="true" onClick={stop}>
                <div className="error-modal-header" style={headerStyle}>Error</div>
                <div className="error-modal-body" style={bodyStyle}>{toPlainString(message)}</div>
                <div className="error-modal-actions" style={buttonRow}>
                    <div style={leftGroup}>
                        <button
                            type="button"
                            className="error-modal-copy"
                            onClick={handleCopy}
                            style={copyBtn}
                            aria-label="Copy error message"
                        >
                            {copied ? 'Copied!' : 'Copy'}
                        </button>
                    </div>
                    <div style={rightGroup}>
                        <button className="error-modal-close" onClick={onDismiss} style={closeBtn}>Close</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

/**
 * Custom hook for error handling
 * @returns {object} - Contains ErrorDisplay component and control methods
 */
export const useError = () => {
    const [errorMessage, setErrorMessage] = useState(null);

    const showError = useCallback((message) => {
        setErrorMessage(message);
    }, []);

    const dismissError = useCallback(() => {
        setErrorMessage(null);
    }, []);

    const MemoizedErrorDisplay = useMemo(() => () => (
        <ErrorDisplay message={errorMessage} onDismiss={dismissError} />
    ), [errorMessage, dismissError]);

    return { ErrorDisplay: MemoizedErrorDisplay, showError, dismissError };
};

export default ErrorDisplay;