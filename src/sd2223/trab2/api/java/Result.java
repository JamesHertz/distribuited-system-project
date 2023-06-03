package sd2223.trab2.api.java;

/**
 * 
 * Represents the result of an operation, either wrapping a result of the given type,
 * or an error.
 * 
 * @author smd
 *
 * @param <T> type of the result value associated with success
 */
public interface Result<T> {

	/**
	 * 
	 * @author smd
	 *
	 * Service errors:
	 * OK - no error, implies a non-null result of type T, except for for Void operations
	 * CONFLICT - something is being created but already exists
	 * NOT_FOUND - an access occurred to something that does not exist
	 * INTERNAL_ERROR - something unexpected happened
	 */
	enum ErrorCode{ OK, CONFLICT, NOT_FOUND, BAD_REQUEST, FORBIDDEN, INTERNAL_ERROR, REDIRECTED, NOT_IMPLEMENTED, TIMEOUT, NO_CONTENT, SERVICE_UNAVAILABLE};

	
	/**
	 * Tests if the result is an error.
	 */
	boolean isOK();
	
	/**
	 * obtains the payload value of this result
	 * @return the value of this result.
	 */
	T value();

	/**
	 *
	 * obtains the error code of this result
	 * @return the error code
	 * 
	 */
	ErrorCode error();

	/**
	 * Convenience method for returning non error results of the given type
	 * @param <T> of value of the result
	 * @return the value of the result
	 */
	static <T> Result<T> ok( T result ) {
		return new OkResult<>(result);
	}

	/**
	 * Convenience method for returning non error results without a value
	 * @return non-error result
	 */
	static <T> Result<T> ok() {
		return new OkResult<>(null);	
	}
	
	/**
	 * Convenience method used to return an error 
	 * @return
	 */
	static <T> Result<T> error(ErrorCode error) {
		return new ErrorResult<>(error);
	}

	static <T> Result<T> error(int status) {
		return error(
				ErrorResult.getErrorCodeFrom(status)
		);
	}

	/**
	 * Convenience method used to return an error
	 * @return
	 */
	static <T> Result<T> error(ErrorCode error, Object errorValue) {
		return new ErrorResult<>(error, errorValue);
	}

	/**
	 * Convenience method used to return an redirect result
	 * @return
	 */
	static <T> Result<T> redirected(Result<T> res) {
		System.err.println(">>>>>>>>>>>" + res );
		if( res.isOK() )
			return error(ErrorCode.REDIRECTED, res.value());
		else
			return res;
	}

}

/*
 * 
 */
class OkResult<T> implements Result<T> {

	final T result;
	
	OkResult(T result) {
		this.result = result;
	}
	
	@Override
	public boolean isOK() {
		return true;
	}

	@Override
	public T value() {
		return result;
	}

	@Override
	public ErrorCode error() {
		return ErrorCode.OK;
	}
	
	public String toString() {
		return "(OK, " + value() + ")";
	}
}

class ErrorResult<T> implements Result<T> {

	final ErrorCode error;
	final Object errorValue;

	ErrorResult(ErrorCode error) {
		this(error, null);
	}

	ErrorResult(ErrorCode error, Object errorValue) {
		this.error = error;
		this.errorValue = errorValue;
	}
	
	@Override
	public boolean isOK() {
		return false;
	}

	@Override
	public ErrorCode error() {
		return error;
	}

	@Override
	public T value() {
		if( error == ErrorCode.REDIRECTED)
			return errorValue();
		throw new RuntimeException("Attempting to extract the value of an Error: " + error());
	}

	@SuppressWarnings("unchecked")
	public <Q> Q errorValue() {
		return (Q)errorValue;
	}

	public String toString() {
		return "(" + error() + ")";		
	}

	public static ErrorCode getErrorCodeFrom(int status) {
		return switch (status) {
			// todo: add timeout :)
			case 200, 209 -> ErrorCode.OK;
			case 409 -> ErrorCode.CONFLICT;
			case 403 -> ErrorCode.FORBIDDEN;
			case 404 -> ErrorCode.NOT_FOUND;
			case 400 -> ErrorCode.BAD_REQUEST;
			case 501 -> ErrorCode.NOT_IMPLEMENTED;
			case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}
}
