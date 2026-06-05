import { Col } from "react-bootstrap";
import "./styles/InformationSection.css";

export default function InformationSection() {
  return (
    <div className="info-container">
      <section className="info-section">
        <Col className="info-column">
          <h2>VERIFIED STUDENTS</h2>
          <p>
            Exclusive to the Mustang community. Secure,
            student-only access via your university credentials.
          </p>
        </Col>
        <Col className="info-column">
          <h2>ON CAMPUS</h2>
          <p>
            Find specialized help, textbooks, and gear just a
            short walk from your dorm or library.
          </p>
        </Col>
        <Col className="info-column">
          <h2>TAILORED CATEGORIES</h2>
          <p>
            Built specifically for student life. Categories
            tailored directly to your major and dorm living
            needs.
          </p>
        </Col>
      </section>
    </div>
  );
}
