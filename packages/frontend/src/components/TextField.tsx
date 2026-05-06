
interface TextFieldProps {
    type: string,
    placeHolder: string,
    onChange: React.ChangeEventHandler<HTMLInputElement>
}

export default function TextField({type, placeHolder, onChange}: TextFieldProps){
    return (
        <input
            style={{
                borderRadius: "8px",
                border: "1px solid #ccc",
                padding: "12px",
                fontSize: "1rem"
            }}
            type={type}
            placeholder={placeHolder}
            onChange={onChange}
        />
    )
}