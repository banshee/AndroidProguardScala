class ProguardCacheParameters
  def initialize args
    @parent_parameters = args[:parent_parameters] or raise "must specify parent parameters"
    newp = args[:new_parameters]
    @new_parameters = case newp
    when Hash
      OpenStruct.new newp
    else
      newp
    end or raise "must specify new parameters"
  end

  def method_missing method, *args
    if @new_parameters.respond_to? method
      @new_parameters.send(method, *args)
    else
      @parent_parameters.send(method, *args)
    end
  end
end