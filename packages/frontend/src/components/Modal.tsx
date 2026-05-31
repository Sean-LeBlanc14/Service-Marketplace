
import './styles/Modal.css'; 

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    children: React.ReactNode;
}

export default function Modal({ isOpen, onClose, children }: ModalProps) {

  if (!isOpen) return null;

  // Close the modal if the user clicks on the darkened backdrop
  const handleOverlayClick = (e) => {
    if (e.target.classList.contains('modal-overlay')) {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-box">
        <button className="close-btn" onClick={onClose}>
          &times;
        </button>
        <div>
          {children}
        </div>
      </div>
    </div>
  );
}