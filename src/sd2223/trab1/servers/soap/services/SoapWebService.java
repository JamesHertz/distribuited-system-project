package sd2223.trab1.servers.soap.services;

import java.util.function.Function;

import sd2223.trab1.api.java.Result;

public abstract class SoapWebService<E extends Throwable> {

	Function<Result<?>, E> exceptionMapper;
	
	SoapWebService( Function<Result<?>, E> exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}
	
	/*
	 * Given a Result<T> returns T value or throws an exception created using the
	 * given function
	 */
	<T> T fromJavaResult(Result<T> result) throws E {
		if (result.isOK() || result.error() == Result.ErrorCode.NO_CONTENT) // this is how we code :)
			return result.value();
		else
			throw exceptionMapper.apply(result);
	}
}
