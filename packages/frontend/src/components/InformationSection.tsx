import { Col } from "react-bootstrap";


export default function InformationSection(){
    return <div
        style={{
          flex: "1",
          backgroundColor: "#f4f4f4",
          padding: "60px"
        }}>
        <section
          style={{
            flex: "1",
            display: "flex",
            flexDirection: "row",
            justifyContent: "center",
            alignItems: "center",
            gap: "40px",
            maxWidth: "1000px",
            margin: "0 auto"
          }}>
          <Col
            style={{
              flex: 1,
              textAlign: "center",
              maxWidth: "300px"
            }}>
            <h2> Verified Students </h2>
            <p>
              
              Exclusive to the Mustang community. Secure,
              student-only access.
            </p>
          </Col>
          <Col
            style={{
              flex: 1,
              textAlign: "center",
              maxWidth: "300px"
            }}>
            <h2> On Campus </h2>
            <p>
              Your campus, your marketplace. Find specialized
              help and gear just a short walk from your dorm.
            </p>
          </Col>
          <Col
            style={{
              flex: 1,
              textAlign: "center",
              maxWidth: "300px"
            }}>
            <h2> n Categories </h2>
            <p>
              
              Built for student life. Specialized categories
              tailored to your major and dorm needs.
            </p>
          </Col>
        </section>
      </div>
}