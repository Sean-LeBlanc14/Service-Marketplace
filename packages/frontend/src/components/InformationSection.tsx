import { Col } from "react-bootstrap";
import "./Styles/InformationSection.css";

export default function InformationSection() {
  return (
    <div className="info-container">
      <section className="info-section">
        <Col className="info-column">
          <h2> Verified Students </h2>
          <p>
            Exclusive to the Mustang community. Secure,
            student-only access.
          </p>
        </Col>
        <Col className="info-column">
          <h2> On Campus </h2>
          <p>
            Your campus, your marketplace. Find specialized help
            and gear just a short walk from your dorm.
          </p>
        </Col>
        <Col className="info-column">
          <h2> n Categories </h2>
          <p>
            Built for student life. Specialized categories
            tailored to your major and dorm needs.
          </p>
        </Col>
      </section>
    </div>
  );
}
