package com.tonic.api.handlers;

import com.tonic.util.handler.AbstractHandlerBuilder;

/**
 * Builder for creating generic, custom handlers.
 */
public class GenericHandlerBuilder extends AbstractHandlerBuilder<GenericHandlerBuilder>
{
    public static GenericHandlerBuilder get()
    {
        return new GenericHandlerBuilder();
    }
}
